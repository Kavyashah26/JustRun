package org.JustRun.TaskManagementService.Service;


import lombok.RequiredArgsConstructor;
import org.JustRun.TaskManagementService.Repository.TaskRepository;
import org.JustRun.TaskManagementService.dto.TaskRequest;
import org.JustRun.TaskManagementService.dto.TaskResponse;
import org.JustRun.TaskManagementService.model.Task;
import org.JustRun.TaskManagementService.model.TaskChain;
import org.JustRun.TaskManagementService.model.TaskPriority;
import org.JustRun.TaskManagementService.model.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

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
                .priority(TaskPriority.valueOf(request.getPriority()))
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


        // Schedule the task
//        schedulerService.scheduleTask(savedTask);

        return savedTask;
    }

}