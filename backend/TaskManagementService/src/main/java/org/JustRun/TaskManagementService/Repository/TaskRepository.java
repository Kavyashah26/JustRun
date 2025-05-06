package org.JustRun.TaskManagementService.Repository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.JustRun.TaskManagementService.model.Task;
import org.JustRun.TaskManagementService.model.TaskChain;
import org.JustRun.TaskManagementService.model.TaskPriority;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class TaskRepository {

    private final DynamoDbClient dynamoDbClient;
    private static final String TABLE_NAME = "tasks";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Task save(Task task) {
        if (task.getId() == null) {
            task.setId(UUID.randomUUID().toString());
            task.setCreatedAt(LocalDateTime.now());
        }

        task.setUpdatedAt(LocalDateTime.now());

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s(task.getId()).build());
        item.put("name", AttributeValue.builder().s(task.getName()).build());
        item.put("userId", AttributeValue.builder().s(task.getUserId()).build());
        item.put("endpoint", AttributeValue.builder().s(task.getEndpoint()).build());
        item.put("method", AttributeValue.builder().s(task.getMethod()).build());
        item.put("cronExpression", AttributeValue.builder().s(task.getCronExpression()).build());
        item.put("priority", AttributeValue.builder().s(task.getPriority().name()).build());
        item.put("status", AttributeValue.builder().s(task.getStatus()).build());
        item.put("createdAt", AttributeValue.builder().s(task.getCreatedAt().format(DATE_FORMATTER)).build());
        item.put("updatedAt", AttributeValue.builder().s(task.getUpdatedAt().format(DATE_FORMATTER)).build());

        if (task.getDescription() != null) {
            item.put("description", AttributeValue.builder().s(task.getDescription()).build());
        }

        if (task.getBody() != null) {
            Map<String, AttributeValue> bodyMap = new HashMap<>();
            task.getBody().forEach((key, value) -> {
                bodyMap.put(key, AttributeValue.builder().s(value.toString()).build()); // or handle types more carefully if needed
            });
            item.put("body", AttributeValue.builder().m(bodyMap).build());

        }

        if (task.getMaxRetries() != null) {
            item.put("maxRetries", AttributeValue.builder().n(task
                    .getMaxRetries().toString()).build());
        }

        if (task.getRetryDelay() != null) {
            item.put("retryDelay", AttributeValue.builder().n(task.getRetryDelay().toString()).build());
        }

        if (task.getExponentialBackoff() != null) {
            item.put("exponentialBackoff", AttributeValue.builder().bool(task.getExponentialBackoff()).build());
        }

        if (task.getWebhookUrl() != null) {
            item.put("webhookUrl", AttributeValue.builder().s(task.getWebhookUrl()).build());
        }

        if (task.getLastExecutedAt() != null) {
            item.put("lastExecutedAt", AttributeValue.builder().s(task.getLastExecutedAt().format(DATE_FORMATTER)).build());
        }

        if (task.getExecutionCount() != null) {
            item.put("executionCount", AttributeValue.builder().n(task.getExecutionCount().toString()).build());
        }

        if (task.getFailureCount() != null) {
            item.put("failureCount", AttributeValue.builder().n(task.getFailureCount().toString()).build());
        }

        // Store headers as JSON
        if (task.getHeaders() != null && !task.getHeaders().isEmpty()) {
            Map<String, AttributeValue> headersMap = new HashMap<>();
            task.getHeaders().forEach((key, value) ->
                    headersMap.put(key, AttributeValue.builder().s(value).build())
            );
            item.put("headers", AttributeValue.builder().m(headersMap).build());
        }

        // Store chains as a list
        if (task.getChains() != null && !task.getChains().isEmpty()) {
            List<AttributeValue> chains = new ArrayList<>();
            for (TaskChain chain : task.getChains()) {
                Map<String, AttributeValue> chainMap = new HashMap<>();
                chainMap.put("id", AttributeValue.builder().s(chain.getId()).build());
                chainMap.put("taskId", AttributeValue.builder().s(chain.getTaskId()).build());
                chainMap.put("statusCode", AttributeValue.builder().n(chain.getStatusCode().toString()).build());
                chainMap.put("nextTaskId", AttributeValue.builder().s(chain.getNextTaskId()).build());

                chains.add(AttributeValue.builder().m(chainMap).build());
            }
            item.put("chains", AttributeValue.builder().l(chains).build());
        }

        PutItemRequest request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();

        dynamoDbClient.putItem(request);
        return task;
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
