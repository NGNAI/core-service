-- V29: Mở rộng cột display_name và file_path cho topic_sources & notebook_sources
-- Lý do: tên file dài (>255 ký tự) gây lỗi "value too long for type character varying(255)"
-- khi upload source kèm chat. Đồng bộ với draft_sources (V26) đã dùng VARCHAR(512)/VARCHAR(1024).

ALTER TABLE public.topic_sources
    ALTER COLUMN display_name TYPE VARCHAR(512),
    ALTER COLUMN file_path    TYPE VARCHAR(1024);

ALTER TABLE public.notebook_sources
    ALTER COLUMN display_name TYPE VARCHAR(512),
    ALTER COLUMN file_path    TYPE VARCHAR(1024);
