package org.JustRun.TaskManagementService.Controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.JustRun.TaskManagementService.Service.TaskService;
import org.JustRun.TaskManagementService.dto.TaskRequest;
import org.JustRun.TaskManagementService.dto.TaskResponse;
import org.JustRun.TaskManagementService.model.Task;
import org.JustRun.TaskManagementService.model.TaskChain;
import org.JustRun.TaskManagementService.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal User user) {
        Task task = taskService.createTask(request, user);
        return ResponseEntity.ok(mapToTaskResponse(task));
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> getUserTasks(@AuthenticationPrincipal User user) {
        List<Task> tasks = taskService.getUserTasks(user.getId());
        List<TaskResponse> response = tasks.stream()
                .map(this::mapToTaskResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTask(@PathVariable String id, @AuthenticationPrincipal User user) {
        Task task = taskService.getTask(id, user.getId());
        return ResponseEntity.ok(mapToTaskResponse(task));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable String id, @AuthenticationPrincipal User user) {
        taskService.deleteTask(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/executions")
    public ResponseEntity<List<TaskResponse.TaskExecutionResponse>> getTaskExecutions(
            @PathVariable String id,
            @AuthenticationPrincipal User user) {
        List<TaskResponse.TaskExecutionResponse> executions = taskService.getTaskExecutions(id, user.getId());
        return ResponseEntity.ok(executions);
    }


    private TaskResponse mapToTaskResponse(Task task) {
//        List<TaskResponse.TaskChainResponse> taskChainResponses = task.getChains().stream()
//                .map(this::mapToTaskChainResponse)  // Map each TaskChain to TaskChainResponse
//                .collect(Collectors.toList());
        // If task.getChains() is null, return an empty list instead of processing further
        List<TaskResponse.TaskChainResponse> taskChainResponses = task.getChains() != null
                ? task.getChains().stream()
                .map(this::mapToTaskChainResponse)  // Map each TaskChain to TaskChainResponse
                .collect(Collectors.toList())
                : Collections.emptyList();  // Return an empty list if chains is null

        return TaskResponse.builder()
                .id(task.getId())
                .name(task.getName())
                .description(task.getDescription())
                .endpoint(task.getEndpoint())
                .method(task.getMethod())
                .headers(task.getHeaders())
                .body(task.getBody())
                .cronExpression(task.getCronExpression())
                .priority(task.getPriority().name())
                .maxRetries(task.getMaxRetries())
                .retryDelay(task.getRetryDelay())
                .exponentialBackoff(task.getExponentialBackoff())
                .webhookUrl(task.getWebhookUrl())
                .status(task.getStatus())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .lastExecutedAt(task.getLastExecutedAt())
                .executionCount(task.getExecutionCount())
                .failureCount(task.getFailureCount())
                .chains(taskChainResponses)
                .nextExecutionTime(task.getNextExecutionTime())
                .taskType(task.getTaskType().name())

                .build();
    }

    private TaskResponse.TaskChainResponse mapToTaskChainResponse(TaskChain taskChain) {
        return TaskResponse.TaskChainResponse.builder()
                .id(taskChain.getId())
                .taskId(taskChain.getTaskId())
                .nextTaskId(taskChain.getNextTaskId())
                .statusCode(taskChain.getStatusCode())
                .build();
    }

}
