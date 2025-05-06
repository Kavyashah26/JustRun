package org.JustRun.TaskExecutionService.Worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.JustRun.TaskExecutionService.model.Task;
import org.JustRun.TaskExecutionService.service.TaskExecutionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueueWorker {

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final TaskExecutionService taskExecutionService;

    @Value("${aws.sqs.high-priority-queue}")
    private String highPriorityQueueUrl;

    @Value("${aws.sqs.normal-priority-queue}")
    private String normalPriorityQueueUrl;

    @Value("${aws.sqs.low-priority-queue}")
    private String lowPriorityQueueUrl;

    @Scheduled(fixedDelay = 5000)
    public void processHighPriorityQueue() {
        log.info("Checking High Priority Queue...");
        processQueue(highPriorityQueueUrl);
    }

    @Scheduled(fixedDelay = 10000)
    public void processNormalPriorityQueue() {
        log.info("Checking Normal Priority Queue...");
        processQueue(normalPriorityQueueUrl);
    }

    @Scheduled(fixedDelay = 20000)
    public void processLowPriorityQueue() {
        log.info("Checking Low Priority Queue...");
        processQueue(lowPriorityQueueUrl);
    }

    private void processQueue(String queueUrl) {
        log.info("Starting to fetch messages from queue: {}", queueUrl);
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(10)
                .visibilityTimeout(30)
                .waitTimeSeconds(5)
                .build();

        try {
            ReceiveMessageResponse response = sqsClient.receiveMessage(receiveRequest);
            List<Message> messages = response.messages();

            log.info("Fetched {} message(s) from queue: {}", messages.size(), queueUrl);

            for (Message message : messages) {
                try {
                    log.debug("Received raw message: {}", message.body());

                    Task task = objectMapper.readValue(message.body(), Task.class);
                    log.info("Deserialized task: {}", task.getId());

                    log.info("Executing task: {}", task.getId());
                    taskExecutionService.executeTask(task);
                    log.info("Successfully executed task: {}", task.getId());

                    DeleteMessageRequest deleteRequest = DeleteMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .receiptHandle(message.receiptHandle())
                            .build();
                    sqsClient.deleteMessage(deleteRequest);
                    log.info("Deleted message from queue: {}", task.getId());

                } catch (IOException e) {
                    log.error("Error deserializing message: {}", e.getMessage(), e);
                } catch (Exception e) {
                    log.error("Error processing task: {}", e.getMessage(), e);
                }
            }

            log.info("Finished processing all messages from queue: {}", queueUrl);
        } catch (Exception e) {
            log.error("Error receiving messages from queue {}: {}", queueUrl, e.getMessage(), e);
        }
    }
}
