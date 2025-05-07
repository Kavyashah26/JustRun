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

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueService {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

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

            SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageBody)
                    .messageGroupId(task.getId())
                    .build();

            SendMessageResponse response = sqsClient.sendMessage(sendMessageRequest);

            log.info("Task [{}] successfully sent to SQS queue [{}]. Message ID: {}, HTTP Status: {}",
                    task.getId(), queueUrl, response.messageId(), response.sdkHttpResponse().statusCode());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize task [{}]: {}", task.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to serialize task", e);
        } catch (Exception e) {
            log.error("Unexpected error while enqueuing task [{}]: {}", task.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to enqueue task", e);
        }
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

