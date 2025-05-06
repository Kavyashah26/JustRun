package org.JustRun.TaskManagementService.Repository;


import lombok.RequiredArgsConstructor;
import org.JustRun.TaskManagementService.model.User;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final DynamoDbClient dynamoDbClient;
    private static final String TABLE_NAME = "users_auth";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public User save(User user) {
        if (user.getId() == null) {
            user.setId(UUID.randomUUID().toString());
            user.setCreatedAt(LocalDateTime.now());
        }

        user.setUpdatedAt(LocalDateTime.now());

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id", AttributeValue.builder().s(user.getId()).build());
        item.put("username", AttributeValue.builder().s(user.getUsername()).build());
        item.put("email", AttributeValue.builder().s(user.getEmail()).build());
        item.put("password", AttributeValue.builder().s(user.getPassword()).build());
        item.put("createdAt", AttributeValue.builder().s(user.getCreatedAt().format(DATE_FORMATTER)).build());
        item.put("updatedAt", AttributeValue.builder().s(user.getUpdatedAt().format(DATE_FORMATTER)).build());
        item.put("enabled", AttributeValue.builder().bool(user.isEnabled()).build());

        PutItemRequest request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();

        dynamoDbClient.putItem(request);
        return user;
    }

    public Optional<User> findByUsername(String username) {
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":username", AttributeValue.builder().s(username).build());

        ScanRequest request = ScanRequest.builder()
                .tableName(TABLE_NAME)
                .filterExpression("username = :username")
                .expressionAttributeValues(expressionValues)
                .build();

        ScanResponse response = dynamoDbClient.scan(request);

        if (response.items().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(mapToUser(response.items().get(0)));
    }

    public Optional<User> findByEmail(String email) {
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":email", AttributeValue.builder().s(email).build());

        ScanRequest request = ScanRequest.builder()
                .tableName(TABLE_NAME)
                .filterExpression("email = :email")
                .expressionAttributeValues(expressionValues)
                .build();

        ScanResponse response = dynamoDbClient.scan(request);

        if (response.items().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(mapToUser(response.items().get(0)));
    }


    private User mapToUser(Map<String, AttributeValue> item) {
        return User.builder()
                .id(item.get("id").s())
                .username(item.get("username").s())
                .email(item.get("email").s())
                .password(item.get("password").s())
                .createdAt(LocalDateTime.parse(item.get("createdAt").s(), DATE_FORMATTER))
                .updatedAt(LocalDateTime.parse(item.get("updatedAt").s(), DATE_FORMATTER))
                .enabled(item.get("enabled").bool())
                .build();
    }
}

