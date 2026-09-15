-- V32: Theo dõi trạng thái sinh source-guide summary cho notebook source (NotebookLM).
--
-- Vấn đề: trước đây scheduler `syncCompletedSourceGuides` quét mọi source COMPLETED
-- nhưng thiếu summary rồi poll GET liên tục, không có giới hạn retry. Nếu guide không
-- bao giờ được tạo (trigger thất bại, hoặc source có trước khi bật tính năng) thì
-- scheduler poll vô hạn mỗi phút, gây tải vô ích lên RAG service và log nhiễu.
--
-- Các cột thêm vào:
--   summary_status      : trạng thái sinh summary — NULL (chưa trigger) / PROCESSING /
--                         COMPLETED / FAILED.
--   summary_retry_count : số lần đã thử trigger/poll thất bại liên tiếp, dùng để giới
--                         hạn retry (giống dispatch_retry_count).
--   summary_error       : thông báo lỗi gần nhất từ RAG service, giúp debug mà không
--                         cần tra log.
ALTER TABLE public.notebook_sources
    ADD COLUMN IF NOT EXISTS summary_status varchar(32),
    ADD COLUMN IF NOT EXISTS summary_retry_count int4 NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS summary_error TEXT;

COMMENT ON COLUMN public.notebook_sources.summary_status IS
    'Trạng thái sinh source-guide summary: NULL/PROCESSING/COMPLETED/FAILED';
COMMENT ON COLUMN public.notebook_sources.summary_retry_count IS
    'Số lần thử sinh summary thất bại liên tiếp (giới hạn retry của scheduler)';
COMMENT ON COLUMN public.notebook_sources.summary_error IS
    'Lỗi gần nhất khi sinh summary (body/thông báo từ RAG service)';

-- Index hỗ trợ truy vấn của scheduler: tìm source COMPLETED thiếu summary còn retry được
CREATE INDEX IF NOT EXISTS idx_notebook_sources_summary_tracking
    ON public.notebook_sources (vector_status, delete_status, summary_status, summary_retry_count);

-- Backfill: source đã có summary coi như hoàn thành
UPDATE public.notebook_sources
SET summary_status = 'COMPLETED'
WHERE summary IS NOT NULL AND summary <> '' AND summary_status IS NULL;
