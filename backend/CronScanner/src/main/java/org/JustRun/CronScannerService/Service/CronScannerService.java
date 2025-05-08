package org.JustRun.CronScannerService.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.JustRun.CronScannerService.Model.Task;
import org.JustRun.CronScannerService.Repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CronScannerService {

    private final TaskRepository taskRepository;
    private final QueueService queueService;
    private final PostHogService postHogService;


    //    @Scheduled(cron = "0 * * * * ?")  // Executes every minute
    @Scheduled(fixedRate = 2*30000)
    public void scanAndEnqueueDueTasks() {
        log.info("Starting to scan for due tasks...");
        Map<String, Object> properties = new HashMap<>();
        properties.put("action", "DB check");
        properties.put("service", "CronScannerService");
        postHogService.trackEvent("CronScannerService", "DB Checked", properties);
        List<Task> dueTasks = taskRepository.findDueCronTasks();

        if (dueTasks.isEmpty()) {
            log.info("No tasks are due for execution.");
        } else {
            log.info("Found {} due task(s).", dueTasks.size());

            // Enqueue each due task to the queue
            for (Task task : dueTasks) {
                log.info("Trying to claim task: {}", task.getId());

                LocalDateTime oldNextTime = task.getNextExecutionTime();

                boolean claimed = taskRepository.claimDueTask(task, oldNextTime);

                if(claimed) {
                    Map<String, Object> taskProperties = new HashMap<>();
                    taskProperties.put("taskId", task.getId());
                    taskProperties.put("taskName", task.getName());
                    taskProperties.put("priority", task.getPriority().name());
                    taskProperties.put("cron", task.getCronExpression());

                    // Track event for each task being claimed
                    postHogService.trackEvent(task.getId(), "task_claimed", taskProperties);
                    System.out.println("[Analytics] Event 'task_claimed' sent to PostHog successfully.");

                    log.info("calling queueservice for task: {}", task.getId());
                    queueService.enqueueTask(task);
                }
            }
        }
    }

}
