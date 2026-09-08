-- V30: Mở rộng cột varchar(255) có nguy cơ tràn + bổ sung index còn thiếu
-- ============================================================================
-- Lý do: tên file dài (>255 ký tự) gây lỗi "value too long for type character varying(255)".
-- Mở rộng toàn bộ cột String chứa dữ liệu người dùng nhập tự do (title/name/description/email/path)
-- để tránh lỗi tương tự. Các cột enum (source_type, access_level, vector_status, type...) giữ nguyên
-- vì giá trị cố định ngắn.
-- ============================================================================

-- ────────────────────────────────────────────────────────────────────────────
-- 1. MỞ RỘNG CỘT — rủi ro CAO (tên file / đường dẫn MinIO)
-- ────────────────────────────────────────────────────────────────────────────
ALTER TABLE public.data_ingestion
    ALTER COLUMN name       TYPE VARCHAR(1024),
    ALTER COLUMN minio_path TYPE VARCHAR(1024);

-- ────────────────────────────────────────────────────────────────────────────
-- 2. MỞ RỘNG CỘT — rủi ro TRUNG BÌNH (feedback người dùng)
-- ────────────────────────────────────────────────────────────────────────────
ALTER TABLE public.message
    ALTER COLUMN feedback TYPE TEXT;

ALTER TABLE public.message_feedback_history
    ALTER COLUMN before_feedback TYPE TEXT,
    ALTER COLUMN after_feedback  TYPE TEXT;

-- ────────────────────────────────────────────────────────────────────────────
-- 3. MỞ RỘNG CỘT — rủi ro THẤP (title/name/description/email/path)
-- ────────────────────────────────────────────────────────────────────────────
ALTER TABLE public.topic
    ALTER COLUMN title TYPE VARCHAR(512);

ALTER TABLE public.notebook
    ALTER COLUMN title TYPE VARCHAR(512);

ALTER TABLE public.draft
    ALTER COLUMN title TYPE VARCHAR(512);

ALTER TABLE public.note
    ALTER COLUMN title TYPE VARCHAR(512);

ALTER TABLE public.users
    ALTER COLUMN email        TYPE VARCHAR(512),
    ALTER COLUMN first_name   TYPE VARCHAR(512),
    ALTER COLUMN last_name    TYPE VARCHAR(512),
    ALTER COLUMN phone_number TYPE VARCHAR(512);

ALTER TABLE public.organizations
    ALTER COLUMN name        TYPE VARCHAR(512),
    ALTER COLUMN description TYPE VARCHAR(512),
    ALTER COLUMN path        TYPE VARCHAR(1024);

ALTER TABLE public.role
    ALTER COLUMN name        TYPE VARCHAR(512),
    ALTER COLUMN description TYPE VARCHAR(512);

ALTER TABLE public.permission
    ALTER COLUMN name            TYPE VARCHAR(512),
    ALTER COLUMN description     TYPE VARCHAR(512),
    ALTER COLUMN code            TYPE VARCHAR(512),
    ALTER COLUMN resource        TYPE VARCHAR(512),
    ALTER COLUMN action          TYPE VARCHAR(512),
    ALTER COLUMN target_resource TYPE VARCHAR(512);

-- ────────────────────────────────────────────────────────────────────────────
-- 4. BỔ SUNG INDEX CÒN THIẾU
-- ────────────────────────────────────────────────────────────────────────────
-- topic: list theo user/org
CREATE INDEX IF NOT EXISTS idx_topic_owner_id         ON public.topic (owner_id);
CREATE INDEX IF NOT EXISTS idx_topic_organization_id  ON public.topic (organization_id);

-- notebook: list theo user/org
CREATE INDEX IF NOT EXISTS idx_notebook_owner_id         ON public.notebook (owner_id);
CREATE INDEX IF NOT EXISTS idx_notebook_organization_id  ON public.notebook (organization_id);

-- organizations: truy vấn cây theo parent
CREATE INDEX IF NOT EXISTS idx_organizations_parent_id ON public.organizations (parent_id);
