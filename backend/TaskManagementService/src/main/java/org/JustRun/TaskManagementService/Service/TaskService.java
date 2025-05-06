package org.JustRun.TaskManagementService.Service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.JustRun.TaskManagementService.Repository.TaskRepository;
import org.JustRun.TaskManagementService.dto.TaskRequest;
import org.JustRun.TaskManagementService.dto.TaskResponse;
import org.JustRun.TaskManagementService.exceptions.ResourceNotFoundException;
import org.JustRun.TaskManagementService.exceptions.UnauthorizedException;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final QueueService queueService;

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
        try{
            queueService.enqueueTask(savedTask);
        }catch (Exception e){
            log.error("Failed to enqueue task: {}", savedTask.getId(), e);
        }

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

}