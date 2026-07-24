-- V16: Tạo bảng share_links cho tính năng public share link (Topic / Notebook)
-- Viewer truy cập qua /public/share/{token} (read-only, không cần JWT)
-- Cơ chế bảo mật: token ngẫu nhiên 32 byte (base64 URL-safe) + password tùy chọn (BCrypt) + expiry tùy chọn

CREATE TABLE IF NOT EXISTS public.share_links (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token           VARCHAR(64)  NOT NULL UNIQUE,
    resource_type   VARCHAR(32)  NOT NULL,
    resource_id     UUID         NOT NULL,
    owner_id        UUID         NOT NULL,
    organization_id UUID         NOT NULL,
    title           VARCHAR(256),
    password_hash   VARCHAR(100),
    expires_at      TIMESTAMP,
    revoked_at      TIMESTAMP,
    view_count      BIGINT       NOT NULL DEFAULT 0,
    last_viewed_at  TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    created_by      UUID,
    updated_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by      UUID
);

-- Index unique trên token (lookup public access)
CREATE UNIQUE INDEX IF NOT EXISTS idx_share_links_token ON public.share_links (token);

-- Index trên (resource_type, resource_id) — lookup link theo resource
CREATE INDEX IF NOT EXISTS idx_share_links_resource ON public.share_links (resource_type, resource_id);

-- Index trên owner_id — list link của owner
CREATE INDEX IF NOT EXISTS idx_share_links_owner ON public.share_links (owner_id);

-- Comment cho documentation
COMMENT ON TABLE  public.share_links IS 'Public share link cho Topic / Notebook (read-only viewer)';
COMMENT ON COLUMN public.share_links.token IS 'Token ngẫu nhiên 32 byte base64 URL-safe, không đánh đoán được';
COMMENT ON COLUMN public.share_links.password_hash IS 'BCrypt hash, NULL = không yêu cầu password';
COMMENT ON COLUMN public.share_links.expires_at IS 'Thời điểm hết hạn, NULL = vĩnh viễn';
COMMENT ON COLUMN public.share_links.revoked_at IS 'Thời điểm bị hủy (revoke), NULL = còn hiệu lực';
COMMENT ON COLUMN public.share_links.view_count IS 'Tổng số lượt xem (tăng nguyên tử qua native UPDATE)';