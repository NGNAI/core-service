package ai.scheduler;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ai.service.DataIngestionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DataIngestionMaintenanceScheduler {
    DataIngestionService dataIngestionService;

    AtomicBoolean ingestionStatusSyncRunning = new AtomicBoolean(false);
    AtomicBoolean pendingDeleteWorkerRunning = new AtomicBoolean(false);

    @Scheduled(cron = "0 0/1 * * * ?")
    public void syncIngestionStatuses() {
        if (!ingestionStatusSyncRunning.compareAndSet(false, true)) {
            return;
        }

        try {
            dataIngestionService.syncPendingIngestionStatuses();
        } catch (Exception exception) {
            log.error("Error while syncing ingestion statuses", exception);
        } finally {
            ingestionStatusSyncRunning.set(false);
        }
    }

    @Scheduled(cron = "0 0/1 * * * ?")
    public void processPendingDataIngestionDeletes() {
        if (!pendingDeleteWorkerRunning.compareAndSet(false, true)) {
            return;
        }

        try {
            dataIngestionService.processPendingDeleteQueue();
        } catch (Exception exception) {
            log.error("Error while processing pending data ingestion deletions", exception);
        } finally {
            pendingDeleteWorkerRunning.set(false);
        }
    }
}
