package org.JustRun.TaskExecutionService.Repository;


import lombok.RequiredArgsConstructor;
import org.JustRun.TaskExecutionService.model.TaskExecution;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class TaskExecutionRepository {

    private final DynamoDbClient dynamoDbClient;
    private static final String TABLE_NAME = "task_executions";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public TaskExecution save(TaskExecution execution) {
        if (execution.getId() == null) {
            execution.setId(UUID.randomUUID().toString());
        }

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s(execution.getId()).build());
        item.put("taskId", AttributeValue.builder().s(execution.getTaskId()).build());
        item.put("executionTime", AttributeValue.builder().s(execution.getExecutionTime().format(DATE_FORMATTER)).build());
        item.put("status", AttributeValue.builder().s(execution.getStatus()).build());

        if (execution.getStatusCode() != null) {
            item.put("statusCode", AttributeValue.builder().n(execution.getStatusCode().toString()).build());
        }

        if (execution.getResponse() != null) {
            item.put("response", AttributeValue.builder().s(execution.getResponse()).build());
        }

        if (execution.getError() != null) {
            item.put("error", AttributeValue.builder().s(execution.getError()).build());
        }

        if (execution.getRetryCount() != null) {
            item.put("retryCount", AttributeValue.builder().n(execution.getRetryCount().toString()).build());
        }

        if (execution.getNextRetry() != null) {
            item.put("nextRetry", AttributeValue.builder().s(execution.getNextRetry().format(DATE_FORMATTER)).build());
        }

        PutItemRequest request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();

        dynamoDbClient.putItem(request);
        return execution;
    }

    private TaskExecution mapToTaskExecution(Map<String, AttributeValue> item) {
        TaskExecution execution = new TaskExecution();
        execution.setId(item.get("id").s());
        execution.setTaskId(item.get("taskId").s());
        execution.setExecutionTime(LocalDateTime.parse(item.get("executionTime").s(), DATE_FORMATTER));
        execution.setStatus(item.get("status").s());

        if (item.containsKey("statusCode")) {
            execution.setStatusCode(Integer.parseInt(item.get("statusCode").n()));
        }

        if (item.containsKey("response")) {
            execution.setResponse(item.get("response").s());
        }

        if (item.containsKey("error")) {
            execution.setError(item.get("error").s());
        }

        if (item.containsKey("retryCount")) {
            execution.setRetryCount(Integer.parseInt(item.get("retryCount").n()));
        }

        if (item.containsKey("nextRetry")) {
            execution.setNextRetry(LocalDateTime.parse(item.get("nextRetry").s(), DATE_FORMATTER));
        }

        return execution;
    }
}