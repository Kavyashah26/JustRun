package org.JustRun.TaskManagementService.Controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.JustRun.TaskManagementService.Service.TaskService;
import org.JustRun.TaskManagementService.dto.TaskRequest;
import org.JustRun.TaskManagementService.dto.TaskResponse;
import org.JustRun.TaskManagementService.model.Task;
import org.JustRun.TaskManagementService.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    private TaskResponse mapToTaskResponse(Task task) {
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
                .build();
    }
}
