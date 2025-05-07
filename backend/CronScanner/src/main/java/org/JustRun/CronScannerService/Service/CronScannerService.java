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
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CronScannerService {

    private final TaskRepository taskRepository;
    private final QueueService queueService;

//    @Scheduled(cron = "0 * * * * ?")  // Executes every minute
    @Scheduled(fixedRate = 2*30000)
    public void scanAndEnqueueDueTasks() {
        log.info("Starting to scan for due tasks...");
        List<Task> dueTasks = taskRepository.findDueCronTasks();

        if (dueTasks.isEmpty()) {
            log.info("No tasks are due for execution.");
        } else {
            log.info("Found {} due task(s).", dueTasks.size());

            // Enqueue each due task to the queue
            for (Task task : dueTasks) {
                log.info("calling queueservice for task: {}", task.getId());
                queueService.enqueueTask(task);
            }
        }
    }

}
