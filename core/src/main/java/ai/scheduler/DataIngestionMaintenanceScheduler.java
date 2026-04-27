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

    /**
     * Thực hiện đồng bộ trạng thái của các công việc data ingestion đang ở trạng thái trung gian (ví dụ: PROCESSING) sang trạng thái cuối cùng (COMPLETED hoặc FAILED) nếu đã quá thời gian dự kiến hoàn thành. Điều này giúp đảm bảo rằng trạng thái của các công việc data ingestion luôn được cập nhật chính xác, ngay cả khi có sự cố xảy ra trong quá trình xử lý.    
     */
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

    /**
     * Xử lý hàng đợi các yêu cầu xóa data ingestion đã được chấp nhận nhưng chưa được xử lý. Scheduler này sẽ chạy định kỳ để kiểm tra và thực hiện các yêu cầu xóa đang chờ xử lý, đảm bảo rằng các mục data ingestion được xóa đúng cách và kịp thời, đồng thời phát sinh các sự kiện hệ thống tương ứng (ví dụ: DATA_INGESTION_DELETED hoặc DATA_INGESTION_DELETE_FAILED) để thông báo về kết quả của quá trình xóa.
     */
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
