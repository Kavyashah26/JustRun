package org.JustRun.CronScannerService.Service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.JustRun.CronScannerService.Model.Task;
import org.JustRun.CronScannerService.Model.TaskPriority;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueService {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final PostHogService postHogService;

    @Value("${aws.sqs.high-priority-queue}")
    private String highPriorityQueueUrl;

    @Value("${aws.sqs.normal-priority-queue}")
    private String normalPriorityQueueUrl;

    @Value("${aws.sqs.low-priority-queue}")
    private String lowPriorityQueueUrl;

    public void enqueueTask(Task task) {
        log.info("Enqueuing task with ID: {} and priority: {}", task.getId(), task.getPriority());

        try {
            String queueUrl = getQueueUrlForPriority(task.getPriority());
            log.debug("Determined SQS queue URL: {}", queueUrl);

            String messageBody = objectMapper.writeValueAsString(task);
            log.debug("Serialized task to JSON: {}", messageBody);
            String deduplicationId = task.getId() + "-" + System.currentTimeMillis();

            SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBody)
                    .messageGroupId(task.getId())
                    .messageDeduplicationId(deduplicationId)
                    .build();

            SendMessageResponse response = sqsClient.sendMessage(sendMessageRequest);

            log.info("Task [{}] successfully sent to SQS queue [{}]. Message ID: {}, HTTP Status: {}",
                    task.getId(), queueUrl, response.messageId(), response.sdkHttpResponse().statusCode());
            Map<String, Object> taskProperties = new HashMap<>();
            taskProperties.put("taskId", task.getId());
            taskProperties.put("taskName", task.getName());
            taskProperties.put("priority", task.getPriority().name());
            taskProperties.put("queue", queueUrl);

            postHogService.trackEvent(task.getId(), "task_enqueued", taskProperties);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize task [{}]: {}", task.getId(), e.getMessage(), e);
            trackFailure(task, "task_enqueuing_failed", "Serialization error");
            throw new RuntimeException("Failed to serialize task", e);
        } catch (Exception e) {
            log.error("Unexpected error while enqueuing task [{}]: {}", task.getId(), e.getMessage(), e);
            trackFailure(task, "task_enqueuing_failed", "Unexpected error");

            throw new RuntimeException("Failed to enqueue task", e);
        }
    }
    private void trackFailure(Task task, String eventName, String errorMessage) {
        // Track failure event in PostHog
        Map<String, Object> taskProperties = new HashMap<>();
        taskProperties.put("taskId", task.getId());
        taskProperties.put("taskName", task.getName());
        taskProperties.put("priority", task.getPriority().name());
        taskProperties.put("error", errorMessage);

        postHogService.trackEvent(task.getId(), eventName, taskProperties);
    }

    private String getQueueUrlForPriority(TaskPriority priority) {
        switch (priority) {
            case HIGH:
                return highPriorityQueueUrl;
            case NORMAL:
                return normalPriorityQueueUrl;
            case LOW:
                return lowPriorityQueueUrl;
            default:
                log.warn("Unknown task priority: {}. Defaulting to NORMAL queue.", priority);
                return normalPriorityQueueUrl;
        }
    }
}

