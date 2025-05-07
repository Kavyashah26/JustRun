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
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskExecutionService {

    private final TaskRepository taskRepository;
    private final TaskExecutionRepository taskExecutionRepository;
//    private final WebhookService webhookService;
    private final QueueService queueService;
    private final RestTemplate restTemplate = new RestTemplate();

    public void executeTask(Task task) {
        log.info("=== [START] Executing task: {} ===", task.getId());

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
            // Prepare HTTP request
            HttpHeaders headers = new HttpHeaders();
            if (task.getHeaders() != null) {
                task.getHeaders().forEach(headers::set);
            }
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonBody = objectMapper.writeValueAsString(task.getBody());
            HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);

            HttpMethod method = HttpMethod.valueOf(task.getMethod());

            log.info("[REQUEST] Sending {} request to {}", method, task.getEndpoint());
            log.debug("Request Headers: {}", headers);
            log.debug("Request Body: {}", task.getBody());

            // Execute HTTP request
            ResponseEntity<String> response = restTemplate.exchange(
                    task.getEndpoint(),
                    method,
                    requestEntity,
                    String.class
            );

            log.info("[RESPONSE] Received status {} for task {}", response.getStatusCodeValue(), task.getId());
            log.debug("Response Body: {}", response.getBody());

            // Update execution with success
            execution.setStatus("COMPLETED");
            execution.setStatusCode(response.getStatusCodeValue());
            execution.setResponse(response.getBody());
            taskExecutionRepository.save(execution);

            updateTaskStats(task, true);

            processTaskChain(task, response.getStatusCodeValue());

//            if (task.getWebhookUrl() != null) {
//                log.info("Sending success webhook to {}", task.getWebhookUrl());
//                webhookService.sendSuccessNotification(task, execution);
//            }

        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.warn("[HTTP ERROR] Request failed for task {} with status {}", task.getId(), ex.getRawStatusCode());
            log.debug("Error Response Body: {}", ex.getResponseBodyAsString());

            execution.setStatus("FAILED");
            execution.setStatusCode(ex.getRawStatusCode());
            execution.setError(ex.getResponseBodyAsString());
            taskExecutionRepository.save(execution);

            updateTaskStats(task, false);
            processTaskChain(task, ex.getRawStatusCode());

            if (shouldRetry(task, execution)) {
                log.info("Retrying task {} due to HTTP error", task.getId());
                scheduleRetry(task, execution);
            }
//            } else if (task.getWebhookUrl() != null) {
//                log.info("Sending failure webhook to {}", task.getWebhookUrl());
//                webhookService.sendFailureNotification(task, execution);
//            }

        } catch (Exception ex) {
            log.error("[UNEXPECTED ERROR] while executing task {}: {}", task.getId(), ex.getMessage(), ex);

            execution.setStatus("FAILED");
            execution.setError("Unexpected error: " + ex.getMessage());
            taskExecutionRepository.save(execution);

            updateTaskStats(task, false);

            if (shouldRetry(task, execution)) {
                log.info("Retrying task {} due to unexpected error", task.getId());
                scheduleRetry(task, execution);
            }
//            } else if (task.getWebhookUrl() != null) {
//                log.info("Sending failure webhook to {}", task.getWebhookUrl());
//                webhookService.sendFailureNotification(task, execution);
//            }
        }
        try {
            String cron = task.getCronExpression();
            if (cron != null && !cron.isEmpty()) {
                CronTrigger cronTrigger = new CronTrigger(cron);
                LocalDateTime baseTime = task.getLastExecutedAt(); // <- Use this
                LocalDateTime now = LocalDateTime.now();

                if (baseTime == null) {
                    log.info("📌 First time executing task {}", task.getId());
                    baseTime = now; // First execution fallback
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
                    log.info("Updating task {} with next execution time: {}", task.getId(), nextExecution);
                    taskRepository.save(task);
                } else {
                    log.warn("Unable to compute next execution time for task {} with cron: {}", task.getId(), cron);
                }
            }
        } catch (Exception e) {
            log.error("Error while calculating next execution time for task {}: {}", task.getId(), e.getMessage(), e);
        }



        log.info("=== [END] Task execution finished: {} ===", task.getId());
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
                }, () -> {
                    log.warn("😶‍🌫️ No next task found for id [{}] and user [{}]", nextTaskId, task.getUserId());
                });

                break; // Process only the first matching chain
            }
        }
    }
}