-- Thêm cột ingestion_error cho bảng data_ingestion
-- Lưu thông báo lỗi (body response dạng string) từ ingestion service (RAG)
-- khi lần ingest gần nhất thất bại, giúp debug nguyên nhân mà không cần tra log.
ALTER TABLE public.data_ingestion
    ADD COLUMN IF NOT EXISTS ingestion_error TEXT;
