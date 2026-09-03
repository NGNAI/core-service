-- V26: Tạo bảng draft_sources cho tính năng upload tài liệu trong Draft chat
-- Tương tự topic_sources nhưng riêng biệt để tránh dữ liệu bị nhiễm chéo
-- Lưu trữ file source được upload kèm draft, dùng chung MinIO bucket knowledgedrafts và ingestion service

CREATE TABLE IF NOT EXISTS public.draft_sources (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    draft_id        UUID         NOT NULL REFERENCES public.draft (id) ON DELETE CASCADE,
    source_type     VARCHAR(32)  NOT NULL,
    display_name    VARCHAR(512) NOT NULL,
    raw_content     TEXT,
    file_path       VARCHAR(1024),
    summary         TEXT,
    metadata        TEXT,
    vector_status   VARCHAR(32)  NOT NULL DEFAULT 'CREATED',
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    created_by      UUID,
    updated_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by      UUID
);

-- Index trên draft_id — lookup sources theo draft
CREATE INDEX IF NOT EXISTS idx_draft_sources_draft_id ON public.draft_sources (draft_id);

-- Comment cho documentation
COMMENT ON TABLE  public.draft_sources IS 'Tài liệu source upload kèm Draft chat (file upload hoặc raw content)';
COMMENT ON COLUMN public.draft_sources.draft_id IS 'FK đến drafts - draft mà source này thuộc về';
COMMENT ON COLUMN public.draft_sources.source_type IS 'Loại source (hiện tại chỉ hỗ trợ FILE)';
COMMENT ON COLUMN public.draft_sources.display_name IS 'Tên hiển thị của file (tên gốc)';
COMMENT ON COLUMN public.draft_sources.raw_content IS 'Nội dung văn bản thô (nếu upload text)';
COMMENT ON COLUMN public.draft_sources.file_path IS 'Đường dẫn file trong MinIO bucket knowledgedrafts';
COMMENT ON COLUMN public.draft_sources.summary IS 'Tóm tắt nội dung (tự động generate)';
COMMENT ON COLUMN public.draft_sources.metadata IS 'Metadata JSON ( MIME type, size, etc.)';
COMMENT ON COLUMN public.draft_sources.vector_status IS 'Trạng thái vector embedding (CREATED, EXTRACTING, CHUNKING, EMBEDDING, STORING, COMPLETED, FAILED)';
