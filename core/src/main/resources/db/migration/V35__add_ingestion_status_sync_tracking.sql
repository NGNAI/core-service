-- V35: Theo dõi số lần đồng bộ trạng thái ingestion thất bại liên tiếp.
--
-- Vấn đề: scheduler `DataIngestionMaintenanceScheduler.syncIngestionStatuses` và
-- `NoteBookIngestionMaintenanceScheduler.syncVectorStatuses` poll trạng thái job trên
-- ingestion service (RAG) mỗi phút. Khi RAG trả 404 (job không còn tồn tại) hoặc mất
-- kết nối liên tục, exception bị bắt và chỉ log lại — trạng thái trong DB vẫn ở dạng
-- non-final nên record vẫn nằm trong tập truy vấn của scheduler → poll vô hạn, log lỗi
-- lặp lại không có hồi kết.
--
-- Cột thêm vào:
--   status_sync_failure_count : số lần đồng bộ trạng thái thất bại LIÊN TIẾP
--                               (lỗi tạm thời: timeout, mất kết nối, 5xx).
--                               Được reset về 0 ngay khi đọc được trạng thái thành công.
--                               Vượt ngưỡng `maintenance.max-status-sync-failures`
--                               (mặc định 5) → đánh dấu FAILED để dừng poll và cho phép
--                               người dùng retry thủ công.
--
-- Lưu ý: trường hợp RAG trả 404 (job không tồn tại) được xử lý tức thời ở tầng service
-- (không cần đếm), vì đây là lỗi vĩnh viễn — poll lại chắc chắn vẫn thất bại.
ALTER TABLE public.data_ingestion
    ADD COLUMN IF NOT EXISTS status_sync_failure_count int4 NOT NULL DEFAULT 0;

ALTER TABLE public.notebook_sources
    ADD COLUMN IF NOT EXISTS status_sync_failure_count int4 NOT NULL DEFAULT 0;

-- Mốc thời gian bắt đầu chờ RAG sinh source-guide (summary_status = PROCESSING).
-- Dùng để phát hiện source-guide kẹt ở trạng thái "processing" quá lâu mà KHÔNG tăng
-- bộ đếm retry (khác với updated_at vì updated_at bị làm mới mỗi lần ghi record).
ALTER TABLE public.notebook_sources
    ADD COLUMN IF NOT EXISTS summary_processing_started_at timestamptz;

COMMENT ON COLUMN public.data_ingestion.status_sync_failure_count IS
    'Số lần đồng bộ trạng thái ingestion thất bại liên tiếp (giới hạn retry của scheduler, tránh poll vô hạn)';

COMMENT ON COLUMN public.notebook_sources.status_sync_failure_count IS
    'Số lần đồng bộ trạng thái vector thất bại liên tiếp (giới hạn retry của scheduler, tránh poll vô hạn)';

COMMENT ON COLUMN public.notebook_sources.summary_processing_started_at IS
    'Thời điểm bắt đầu chờ RAG sinh source-guide; dùng để chặn poll vô hạn khi guide mãi không hoàn thành';

-- Backfill: source đang PROCESSING coi như bắt đầu chờ từ lần cập nhật cuối
UPDATE public.notebook_sources
SET summary_processing_started_at = COALESCE(updated_at, now())
WHERE summary_status = 'PROCESSING' AND summary_processing_started_at IS NULL;
