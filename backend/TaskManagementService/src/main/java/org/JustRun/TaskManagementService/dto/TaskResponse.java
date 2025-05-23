package org.JustRun.TaskManagementService.dto;


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
public class TaskResponse {

    private String id;
    private String name;
    private String description;
    private String endpoint;
    private String method;
    private Map<String, String> headers;
    private Map<String, Object>  body;
    private String cronExpression;
    private String priority;
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
    private List<TaskChainResponse> chains;
    private LocalDateTime nextExecutionTime;
    private String taskType;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskChainResponse {
        private String id;
        private String taskId;
        private Integer statusCode;
        private String nextTaskId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskExecutionResponse {
        private String id;
        private LocalDateTime executionTime;
        private String status;
        private Integer statusCode;
        private String response;
        private String error;
        private Integer retryCount;
        private LocalDateTime nextRetry;

    }
}
