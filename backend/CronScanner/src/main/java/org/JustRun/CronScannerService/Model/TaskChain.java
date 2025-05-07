package org.JustRun.CronScannerService.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskChain {
    private String id;
    private String taskId;
    private Integer statusCode;
    private String nextTaskId;
}