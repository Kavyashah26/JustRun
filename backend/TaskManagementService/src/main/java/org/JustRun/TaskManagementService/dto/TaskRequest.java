package org.JustRun.TaskManagementService.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequest {
    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotBlank(message = "Endpoint is required")
    @Pattern(regexp = "^https?://.*", message = "Endpoint must be a valid URL")
    private String endpoint;

    @NotBlank(message = "Method is required")
    @Pattern(regexp = "^(GET|POST|PUT|DELETE|PATCH)$", message = "Method must be one of: GET, POST, PUT, DELETE, PATCH")
    private String method;

    private Map<String, String> headers;

    private Map<String, Object> body;

//    @NotBlank(message = "Cron expression is required")
    private String cronExpression;

    @NotNull(message = "Priority is required")
    @Pattern(regexp = "^(HIGH|NORMAL|LOW)$", message = "Priority must be one of: HIGH, NORMAL, LOW")
    private String priority;

    private List<TaskChainRequest> chains;

    private Integer maxRetries;

    private Integer retryDelay;

    private Boolean exponentialBackoff;

    @Pattern(regexp = "^https?://.*", message = "Webhook URL must be a valid URL")
    private String webhookUrl;

    @NotNull(message = "Task type is required")
    @Pattern(regexp = "^(ROOT|CHAINED)$", message = "Task type must be either ROOT or CHAINED")
    private String taskType; // Added field

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskChainRequest {
        @NotNull(message = "Status code is required for task chain")
        private Integer statusCode;

        @NotBlank(message = "Next task ID is required for task chain")
        private String nextTaskId;
    }
}
