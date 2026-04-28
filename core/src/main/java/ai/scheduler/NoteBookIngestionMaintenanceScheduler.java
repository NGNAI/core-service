package ai.scheduler;

import ai.service.NoteBookSourceService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NoteBookIngestionMaintenanceScheduler {
    NoteBookSourceService noteBookSourceService;

    AtomicBoolean ingestionStatusSyncRunning = new AtomicBoolean(false);
    AtomicBoolean pendingDeleteWorkerRunning = new AtomicBoolean(false);

    @Scheduled(cron = "0 0/1 * * * ?")
    public void syncNotebookSourceStatuses() {
        if (!ingestionStatusSyncRunning.compareAndSet(false, true)) {
            return;
        }

        try {
            noteBookSourceService.syncPendingVectorStatuses();
        } catch (Exception exception) {
            log.error("Error while syncing notebook source statuses", exception);
        } finally {
            ingestionStatusSyncRunning.set(false);
        }
    }

    @Scheduled(cron = "0 0/1 * * * ?")
    public void processPendingNotebookSourceDeletes() {
        if (!pendingDeleteWorkerRunning.compareAndSet(false, true)) {
            return;
        }

        try {
            noteBookSourceService.processPendingDeleteQueue();
        } catch (Exception exception) {
            log.error("Error while processing pending notebook source deletions", exception);
        } finally {
            pendingDeleteWorkerRunning.set(false);
        }
    }
}
