-- Thêm cột retry_count cho bảng data_ingestion và notebook_sources
-- retry_count / dispatch_retry_count / delete_retry_count: số lần thử lại thất bại liên tiếp,
-- dùng để giới hạn retry trong scheduler (tránh retry vô hạn khi gặp lỗi vĩnh viễn)
ALTER TABLE public.data_ingestion
    ADD COLUMN IF NOT EXISTS retry_count int4 NOT NULL DEFAULT 0;

ALTER TABLE public.notebook_sources
    ADD COLUMN IF NOT EXISTS dispatch_retry_count int4 NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS delete_retry_count int4 NOT NULL DEFAULT 0;
