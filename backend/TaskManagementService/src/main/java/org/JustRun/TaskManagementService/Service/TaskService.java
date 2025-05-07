package org.JustRun.TaskManagementService.Service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.JustRun.TaskManagementService.Repository.TaskExecutionRepository;
import org.JustRun.TaskManagementService.Repository.TaskRepository;
import org.JustRun.TaskManagementService.dto.TaskRequest;
import org.JustRun.TaskManagementService.dto.TaskResponse;
import org.JustRun.TaskManagementService.exceptions.ResourceNotFoundException;
import org.JustRun.TaskManagementService.exceptions.UnauthorizedException;
import org.JustRun.TaskManagementService.model.*;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskExecutionRepository taskExecutionRepository;
//    private final QueueService queueService;

    private Task mapRequestToTask(TaskRequest request) {
        return Task.builder()
                .id(UUID.randomUUID().toString())
                .name(request.getName())
                .description(request.getDescription())
                .endpoint(request.getEndpoint())
                .method(request.getMethod())
                .headers(request.getHeaders())
                .body(request.getBody())
                .cronExpression(request.getCronExpression())
                .priority(request.getPriority() != null ? TaskPriority.valueOf(request.getPriority()) : TaskPriority.NORMAL)
                .maxRetries(request.getMaxRetries())
                .retryDelay(request.getRetryDelay())
                .exponentialBackoff(request.getExponentialBackoff())
                .webhookUrl(request.getWebhookUrl())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public Task createTask(TaskRequest request, User user) {
        Task task = mapRequestToTask(request);
        task.setUserId(user.getId());
        task.setStatus("ACTIVE");
        task.setExecutionCount(0);
        task.setFailureCount(0);

        task.setTaskType(Task.TaskType.valueOf(request.getTaskType().toUpperCase()));
        // Enum value: ROOT or CHAINED

        if (task.getTaskType() == Task.TaskType.CHAINED) {
            task.setCronExpression("");
//            task.setNextExecutionTime(null);
        } else if (task.getTaskType() == Task.TaskType.ROOT) {
            String cron = task.getCronExpression();
            if (cron != null && !cron.isEmpty()) {
                CronTrigger cronTrigger = new CronTrigger(cron);
                LocalDateTime now = LocalDateTime.now();
                Date currentTime = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
                Date nextExecutionDate = cronTrigger.nextExecutionTime(new SimpleTriggerContext(currentTime, null, null));

                if (nextExecutionDate != null) {
                    LocalDateTime nextExecution = nextExecutionDate.toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();
                    if (now.getSecond() != 0) {
                        nextExecution = nextExecution.plusSeconds(now.getSecond());
                    }
                    task.setNextExecutionTime(nextExecution);
                } else {
                    log.warn("Unable to compute next execution time for task {} with cron expression: {}", task.getId(), cron);
                }
            }
        }

        // Process chains if present
        if (request.getChains() != null && !request.getChains().isEmpty()) {
            List<TaskChain> chains = new ArrayList<>();
            for (TaskRequest.TaskChainRequest chainRequest : request.getChains()) {
                TaskChain chain = TaskChain.builder()
                        .id(UUID.randomUUID().toString())
                        .taskId(task.getId()) // Will be set after saving
                        .statusCode(chainRequest.getStatusCode())
                        .nextTaskId(chainRequest.getNextTaskId())
                        .build();
                chains.add(chain);
            }
            task.setChains(chains);
        }

        Task savedTask = taskRepository.save(task);

// Update taskId in chains if present
        if (savedTask.getChains() != null && !savedTask.getChains().isEmpty()) {
            savedTask.getChains().forEach(chain -> chain.setTaskId(savedTask.getId()));
        }

// Save the updated task again after modifying the chains
        taskRepository.save(savedTask);


//        enqueue task after save
//        try{
//            queueService.enqueueTask(savedTask);
//        }catch (Exception e){
//            log.error("Failed to enqueue task: {}", savedTask.getId(), e);
//        }

        return savedTask;
    }

    public List<Task> getUserTasks(String userId) {
        return taskRepository.findByUserId(userId);
    }

    public Task getTask(String id, String userId) {
        Task task = taskRepository.findById(userId,id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));

        if (!task.getUserId().equals(userId)) {
            throw new UnauthorizedException("You don't have permission to access this task");
        }

        return task;
    }

    public void deleteTask(String id, String userId) {
        Task task = getTask(id, userId);

        // Delete the task
        taskRepository.delete(userId,id);
    }

    public List<TaskResponse.TaskExecutionResponse> getTaskExecutions(String id, String userId) {
        // Verify user has access to task
        getTask(id, userId);

        List<TaskExecution> executions = taskExecutionRepository.findByTaskId(id);

        return executions.stream()
                .map(this::mapToExecutionResponse)
                .collect(Collectors.toList());
    }

    private TaskResponse.TaskExecutionResponse mapToExecutionResponse(TaskExecution execution) {
        return TaskResponse.TaskExecutionResponse.builder()
                .id(execution.getId())
                .executionTime(execution.getExecutionTime())
                .status(execution.getStatus())
                .statusCode(execution.getStatusCode())
                .response(execution.getResponse())
                .error(execution.getError())
                .retryCount(execution.getRetryCount())
                .nextRetry(execution.getNextRetry())
                .build();
    }

}