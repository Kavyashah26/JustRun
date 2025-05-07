package org.JustRun.TaskManagementService.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskExecution {
    private String id;
    private String taskId;
    private LocalDateTime executionTime;
    private String status;
    private Integer statusCode;
    private String response;
    private String error;
    private Integer retryCount;
    private LocalDateTime nextRetry;
}