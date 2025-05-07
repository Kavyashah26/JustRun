package org.JustRun.CronScannerService.Repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.JustRun.CronScannerService.Model.Task;
import org.JustRun.CronScannerService.Model.TaskChain;
import org.JustRun.CronScannerService.Model.TaskPriority;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Slf4j
public class TaskRepository {

    private final DynamoDbClient dynamoDbClient;
    private static final String TABLE_NAME = "tasks";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");




    public List<Task> findDueCronTasks() {
        log.info("Starting the task scan for tasks due in the next minute...");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneMinuteFromNow = now.plusMinutes(1);

        // Scan for tasks where nextExecutionTime is in the next minute
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":now", AttributeValue.builder().s(now.format(DATE_FORMATTER)).build());
        expressionValues.put(":oneMinuteFromNow", AttributeValue.builder().s(oneMinuteFromNow.format(DATE_FORMATTER)).build());
        log.info("Scanning with time range: {} to {}", now.format(DATE_FORMATTER), oneMinuteFromNow.format(DATE_FORMATTER));

        ScanRequest request = ScanRequest.builder()
                .tableName(TABLE_NAME)
                .filterExpression("nextExecutionTime BETWEEN :now AND :oneMinuteFromNow")
                .expressionAttributeValues(expressionValues)
                .build();

        ScanResponse response = dynamoDbClient.scan(request);

        log.info("Scan completed. Found {} task(s) in the next minute range.", response.items().size());

        // Filter tasks by cron schedule and nextExecutionTime
        return response.items().stream()
                .map(this::mapToTask)
                .filter(task -> {
                    boolean isDue = isTaskDue(task);
                    if (isDue) {
                        log.info("Task with ID {} is due for execution.", task.getId());
                    } else {
                        log.info("Task with ID {} is not due yet.", task.getId());
                    }
                    return isDue;
                })
                .collect(Collectors.toList());
    }

    private boolean isTaskDue(Task task) {
        log.info("Evaluating if task with ID {} is due for execution.", task.getId());

        // If task has cronExpression, evaluate it
        if (task.getCronExpression() != null && !task.getCronExpression().isEmpty()) {
            log.info("Task with ID {} has a cron expression: {}", task.getId(), task.getCronExpression());

            CronTrigger cronTrigger = new CronTrigger(task.getCronExpression());
            Date nextCronExecutionDate = cronTrigger.nextExecutionTime(new org.springframework.scheduling.support.SimpleTriggerContext());

            // Convert the Date to LocalDateTime
            LocalDateTime nextCronExecution = nextCronExecutionDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            log.info("Task with ID {} next cron execution time: {}", task.getId(), nextCronExecution);

            // If the next execution time of the cron task is less than or equal to the current time, it's due
            return !nextCronExecution.isAfter(LocalDateTime.now().plusMinutes(1));
        }

        // For non-cron tasks, if the nextExecutionTime is in the next minute range, it's due
        boolean isDue = task.getNextExecutionTime() != null && !task.getNextExecutionTime().isAfter(LocalDateTime.now().plusMinutes(1));
        log.info("Task with ID {} next execution time: {}. Is it due? {}", task.getId(), task.getNextExecutionTime(), isDue);

        return isDue;
    }




    private Task mapToTask(Map<String, AttributeValue> item) {
        Task.TaskBuilder builder = Task.builder()
                .id(item.get("id").s())
                .name(item.get("name").s())
                .userId(item.get("userId").s())
                .endpoint(item.get("endpoint").s())
                .method(item.get("method").s())
                .cronExpression(item.get("cronExpression").s())
                .priority(TaskPriority.valueOf(item.get("priority").s()))
                .status(item.get("status").s())
                .createdAt(LocalDateTime.parse(item.get("createdAt").s(), DATE_FORMATTER))
                .updatedAt(LocalDateTime.parse(item.get("updatedAt").s(), DATE_FORMATTER));

        if (item.containsKey("description")) {
            builder.description(item.get("description").s());
        }

        if (item.containsKey("body")) {
            Map<String, AttributeValue> bodyAttrMap = item.get("body").m();
            Map<String, Object> bodyMap = convertAttributeMap(bodyAttrMap);
            builder.body(bodyMap);
        }


        if (item.containsKey("maxRetries")) {
            builder.maxRetries(Integer.parseInt(item.get("maxRetries").n()));
        }

        if (item.containsKey("retryDelay")) {
            builder.retryDelay(Integer.parseInt(item.get("retryDelay").n()));
        }

        if (item.containsKey("exponentialBackoff")) {
            builder.exponentialBackoff(item.get("exponentialBackoff").bool());
        }

        if (item.containsKey("webhookUrl")) {
            builder.webhookUrl(item.get("webhookUrl").s());
        }

        if (item.containsKey("lastExecutedAt")) {
            builder.lastExecutedAt(LocalDateTime.parse(item.get("lastExecutedAt").s(), DATE_FORMATTER));
        }

        if (item.containsKey("executionCount")) {
            builder.executionCount(Integer.parseInt(item.get("executionCount").n()));
        }

        if (item.containsKey("failureCount")) {
            builder.failureCount(Integer.parseInt(item.get("failureCount").n()));
        }

        // Extract headers
        if (item.containsKey("headers")) {
            Map<String, String> headers = new HashMap<>();
            Map<String, AttributeValue> headersMap = item.get("headers").m();

            headersMap.forEach((key, value) -> headers.put(key, value.s()));
            builder.headers(headers);
        }

        // Extract chains
        if (item.containsKey("chains")) {
            List<TaskChain> chains = new ArrayList<>();
            List<AttributeValue> chainsValues = item.get("chains").l();

            for (AttributeValue chainValue : chainsValues) {
                Map<String, AttributeValue> chainMap = chainValue.m();

                TaskChain chain = TaskChain.builder()
                        .id(chainMap.get("id").s())
                        .taskId(chainMap.get("taskId").s())
                        .statusCode(Integer.parseInt(chainMap.get("statusCode").n()))
                        .nextTaskId(chainMap.get("nextTaskId").s())
                        .build();

                chains.add(chain);
            }

            builder.chains(chains);
        }

        if (item.containsKey("nextExecutionTime")) {
            builder.nextExecutionTime(LocalDateTime.parse(item.get("nextExecutionTime").s(), DATE_FORMATTER));
        }

        if (item.containsKey("taskType")) {
            builder.taskType(item.get("taskType").s());
        }

        return builder.build();
    }

    private Map<String, Object> convertAttributeMap(Map<String, AttributeValue> attrMap) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, AttributeValue> entry : attrMap.entrySet()) {
            AttributeValue value = entry.getValue();
            if (value.s() != null) {
                result.put(entry.getKey(), value.s());
            } else if (value.n() != null) {
                result.put(entry.getKey(), Double.parseDouble(value.n())); // or Integer.parseInt() depending on your use case
            } else if (value.bool() != null) {
                result.put(entry.getKey(), value.bool());
            } else if (value.m() != null) {
                result.put(entry.getKey(), convertAttributeMap(value.m())); // recursive call
            } else if (value.l() != null) {
                List<Object> list = new ArrayList<>();
                for (AttributeValue av : value.l()) {
                    if (av.s() != null) list.add(av.s());
                    else if (av.n() != null) list.add(Double.parseDouble(av.n()));
                    else if (av.bool() != null) list.add(av.bool());
                    else if (av.m() != null) list.add(convertAttributeMap(av.m()));
                }
                result.put(entry.getKey(), list);
            }
        }
        return result;
    }


}
