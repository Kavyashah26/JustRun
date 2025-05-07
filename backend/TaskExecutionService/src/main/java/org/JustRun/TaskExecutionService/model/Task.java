package org.JustRun.TaskExecutionService.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    private String id;
    private String name;
    private String description;
    private String userId;
    private String endpoint;
    private String method;
    private Map<String, String> headers;
    private Map<String, Object> body;
    private String cronExpression;
    private TaskPriority priority;
    private List<TaskChain> chains;
    private Integer maxRetries;
    private Integer retryDelay;
    private Boolean exponentialBackoff;
    private String webhookUrl;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastExecutedAt;
    private Integer executionCount;
    private Integer failureCount;
    private LocalDateTime nextExecutionTime;
    private TaskType taskType;

    public enum TaskType {
        ROOT,     // Scheduled by cron
        CHAINED   // Triggered by another task
    }
}