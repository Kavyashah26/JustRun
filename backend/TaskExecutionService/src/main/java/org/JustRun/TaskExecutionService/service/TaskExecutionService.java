package org.JustRun.TaskExecutionService.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.JustRun.TaskExecutionService.Repository.TaskExecutionRepository;
import org.JustRun.TaskExecutionService.Repository.TaskRepository;
import org.JustRun.TaskExecutionService.model.Task;
import org.JustRun.TaskExecutionService.model.TaskChain;
import org.JustRun.TaskExecutionService.model.TaskExecution;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskExecutionService {

    private final TaskRepository taskRepository;
    private final TaskExecutionRepository taskExecutionRepository;
//    private final WebhookService webhookService;
    private final QueueService queueService;
    private final PostHogService postHogService;
    private final RestTemplate restTemplate = new RestTemplate();


public void executeTask(Task task) {
    log.info("🔥 === [START] Executing task: {} ===", task.getId());
    Map<String, Object> startProps = new HashMap<>();
    startProps.put("taskId", task.getId());
    startProps.put("status", "STARTED");
    startProps.put("timestamp", LocalDateTime.now().toString());
    postHogService.trackEvent(task.getUserId(), "task_execution_started", startProps);

    // Create task execution record
    TaskExecution execution = TaskExecution.builder()
            .id(UUID.randomUUID().toString())
            .taskId(task.getId())
            .executionTime(LocalDateTime.now())
            .status("RUNNING")
            .retryCount(0)
            .build();

    execution = taskExecutionRepository.save(execution);

    try {
        // Prepare HTTP request headers
        HttpHeaders headers = new HttpHeaders();
        if (task.getHeaders() != null) {
            task.getHeaders().forEach(headers::set); // Add each header to HttpHeaders
        }

        // Prepare HTTP request body
        String jsonBody = null;
        if (task.getBody() != null) {
            ObjectMapper objectMapper = new ObjectMapper();
            jsonBody = objectMapper.writeValueAsString(task.getBody());
        }

        HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);

        // Log the request body and headers
        log.info("🔥 [REQUEST] Sending {} request to {} with headers: {}",
                HttpMethod.valueOf(task.getMethod()),
                task.getEndpoint(),
                headers);
        log.info("🔥 Request Body: {}", jsonBody);

        // Determine HTTP method
        HttpMethod method = HttpMethod.valueOf(task.getMethod());

        // Execute HTTP request
        ResponseEntity<String> response = restTemplate.exchange(
                task.getEndpoint(),
                method,
                requestEntity,
                String.class
        );

        log.info("🔥 [RESPONSE] Received status {} for task {}", response.getStatusCodeValue(), task.getId());
        log.info("🔥 Response Body: {}", response.getBody());

        // Update execution with success
        execution.setStatus("COMPLETED");
        execution.setStatusCode(response.getStatusCodeValue());
        execution.setResponse(response.getBody());
        taskExecutionRepository.save(execution);

        updateTaskStats(task, true);
        processTaskChain(task, response.getStatusCodeValue());
        Map<String, Object> successProps = new HashMap<>();
        successProps.put("taskId", task.getId());
        successProps.put("status", "COMPLETED");
        successProps.put("statusCode", response.getStatusCodeValue());
        successProps.put("timestamp", LocalDateTime.now().toString());
        postHogService.trackEvent(task.getUserId(), "task_execution_completed", successProps);

    } catch (HttpClientErrorException | HttpServerErrorException ex) {
        log.warn("🔥 [HTTP ERROR] Request failed for task {} with status {}", task.getId(), ex.getRawStatusCode());
        log.info("🔥 Error Response Body: {}", ex.getResponseBodyAsString());
        Map<String, Object> failProps = new HashMap<>();
        failProps.put("taskId", task.getId());
        failProps.put("status", "FAILED");
        failProps.put("errorType", ex.getClass().getSimpleName());
        failProps.put("message", ex.getMessage());
        failProps.put("timestamp", LocalDateTime.now().toString());
        postHogService.trackEvent(task.getUserId(), "task_execution_failed", failProps);

        execution.setStatus("FAILED");
        execution.setStatusCode(ex.getRawStatusCode());
        execution.setError(ex.getResponseBodyAsString());
        taskExecutionRepository.save(execution);

        updateTaskStats(task, false);
        processTaskChain(task, ex.getRawStatusCode());

        if (shouldRetry(task, execution)) {
            log.info("🔥 Retrying task {} due to HTTP error", task.getId());
            scheduleRetry(task, execution);
        }

    } catch (Exception ex) {
        log.error("🔥 [UNEXPECTED ERROR] while executing task {}: {}", task.getId(), ex.getMessage(), ex);
        Map<String, Object> errorProps = new HashMap<>();
        errorProps.put("taskId", task.getId());
        errorProps.put("status", "FAILED");
        errorProps.put("errorType", "UnexpectedError");
        errorProps.put("message", ex.getMessage());
        errorProps.put("timestamp", LocalDateTime.now().toString());
        postHogService.trackEvent(task.getUserId(), "task_execution_error", errorProps);

        execution.setStatus("FAILED");
        execution.setError("Unexpected error: " + ex.getMessage());
        taskExecutionRepository.save(execution);

        updateTaskStats(task, false);

        if (shouldRetry(task, execution)) {
            log.info("🔥 Retrying task {} due to unexpected error", task.getId());
            Map<String, Object> retryProps = new HashMap<>();
            retryProps.put("taskId", task.getId());
            retryProps.put("status", "RETRY_SCHEDULED");
            retryProps.put("reason", "Retry due to previous failure");
            retryProps.put("retryCount", execution.getRetryCount() + 1);
            retryProps.put("timestamp", LocalDateTime.now().toString());
            postHogService.trackEvent(task.getUserId(), "task_retry_scheduled", retryProps);

            scheduleRetry(task, execution);
        }
    }

    // Calculate the next execution time for cron tasks
    try {
        String cron = task.getCronExpression();
        if (cron != null && !cron.isEmpty()) {
            CronTrigger cronTrigger = new CronTrigger(cron);
            LocalDateTime baseTime = task.getLastExecutedAt();
            LocalDateTime now = LocalDateTime.now();

            if (baseTime == null) {
                log.info("🔥 📌 First time executing task {}", task.getId());
                baseTime = now;
            }

            Date baseExecution = Date.from(baseTime.atZone(ZoneId.systemDefault()).toInstant());
            SimpleTriggerContext triggerContext = new SimpleTriggerContext();
            triggerContext.update(baseExecution, baseExecution, baseExecution);

            Date nextExecutionDate = cronTrigger.nextExecutionTime(triggerContext);

            if (nextExecutionDate != null) {
                LocalDateTime nextExecution = nextExecutionDate.toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();

                if (now.getSecond() != 0) {
                    nextExecution = nextExecution.plusSeconds(now.getSecond());
                }

                task.setNextExecutionTime(nextExecution);
                log.info("🔥 Updating task {} with next execution time: {}", task.getId(), nextExecution);
                taskRepository.save(task);
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("taskId", task.getId());
                metadata.put("userId", task.getUserId());
                metadata.put("cron", task.getCronExpression());
                metadata.put("nextExecutionTime", nextExecution.toString());

                postHogService.trackEvent(task.getUserId(), "task_cron_scheduled", metadata);
            } else {
                log.warn("🔥 Unable to compute next execution time for task {} with cron: {}", task.getId(), cron);
            }
        }
    } catch (Exception e) {
        log.error("🔥 Error while calculating next execution time for task {}: {}", task.getId(), e.getMessage(), e);
    }

    log.info("🔥 === [END] Task execution finished: {} ===", task.getId());
}

    private void updateTaskStats(Task task, boolean success) {
        Integer executionCount = task.getExecutionCount() != null ? task.getExecutionCount() : 0;
        task.setExecutionCount(executionCount + 1);
        task.setLastExecutedAt(LocalDateTime.now());

        if (!success) {
            Integer failureCount = task.getFailureCount() != null ? task.getFailureCount() : 0;
            task.setFailureCount(failureCount + 1);
        }

        taskRepository.save(task);
    }

    private boolean shouldRetry(Task task, TaskExecution execution) {
        if (task.getMaxRetries() == null || task.getMaxRetries() <= 0) {
            return false;
        }

        int retryCount = execution.getRetryCount() != null ? execution.getRetryCount() : 0;
        return retryCount < task.getMaxRetries();
    }

    private void scheduleRetry(Task task, TaskExecution execution) {

        int retryCount = execution.getRetryCount() != null ? execution.getRetryCount() : 0;
        retryCount++;

        // Calculate next retry time with exponential backoff if enabled
        int delaySeconds = task.getRetryDelay() != null ? task.getRetryDelay() : 60; // default 60 seconds

        if (task.getExponentialBackoff() != null && task.getExponentialBackoff()) {
            // Apply exponential backoff: delay * 2^retryCount
            delaySeconds = delaySeconds * (int)Math.pow(2, retryCount - 1);
        }

        LocalDateTime nextRetry = LocalDateTime.now().plusSeconds(delaySeconds);

        execution.setRetryCount(retryCount);
        execution.setNextRetry(nextRetry);
        execution.setStatus("RETRY_SCHEDULED");
        taskExecutionRepository.save(execution);

        // Enqueue the task for retry
        queueService.enqueueTask(task);

        log.info("Scheduled retry {} of {} for task {} at {} (delay: {}s)",
                retryCount, task.getMaxRetries(), task.getId(), nextRetry, delaySeconds);

    }

    private void processTaskChain(Task task, int statusCode) {
        if (task.getChains() == null || task.getChains().isEmpty()) {
            return;
        }

        // Find a chain that matches the status code
        for (TaskChain chain : task.getChains()) {
            log.debug("😶‍🌫️ Inspecting task chain: {}", chain);
            if (chain.getStatusCode() == statusCode) {
                String nextTaskId = chain.getNextTaskId();
                log.info("😶‍🌫️ Matching status code found! Task [{}] chains to [{}] on status code [{}]",
                        task.getId(), nextTaskId, statusCode);

                log.info("Processing task chain: {} -> {} for status code {}",
                        task.getId(), nextTaskId, statusCode);

                // Find and execute the next task
                taskRepository.findById(task.getUserId(), nextTaskId).ifPresentOrElse(nextTask -> {
                    log.info("😶‍🌫️ Found next task [{}] - [{}], enqueuing for execution",
                            nextTask.getId(), nextTask.getName());
                    queueService.enqueueTask(nextTask);
                    Map<String, Object> chainedProps = new HashMap<>();
                    chainedProps.put("taskId", task.getId());
                    chainedProps.put("nextTaskId", nextTaskId);
                    chainedProps.put("status", "TASK_CHAINED");
                    chainedProps.put("timestamp", LocalDateTime.now().toString());

                    postHogService.trackEvent(task.getUserId(), "task_chained", chainedProps);

                }, () -> {
                    log.warn("😶‍🌫️ No next task found for id [{}] and user [{}]", nextTaskId, task.getUserId());
                });

                break; // Process only the first matching chain
            }
        }
    }
}