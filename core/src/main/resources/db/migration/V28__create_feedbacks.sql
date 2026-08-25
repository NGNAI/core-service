-- Table: feedbacks
-- Góp ý và phản hồi từ người dùng
CREATE TABLE IF NOT EXISTS public.feedbacks (
    id UUID PRIMARY KEY,
    subject VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    is_private BOOLEAN DEFAULT false,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    response_content TEXT,
    response_date TIMESTAMPTZ,
    sender_id UUID NOT NULL REFERENCES public.users(id),
    sender_org_id UUID NOT NULL REFERENCES public.organizations(id),
    responder_id UUID REFERENCES public.users(id),
    responder_org_id UUID REFERENCES public.organizations(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by UUID NOT NULL
);

-- Index for performance optimization
CREATE INDEX IF NOT EXISTS idx_feedbacks_sender_id ON public.feedbacks(sender_id);
CREATE INDEX IF NOT EXISTS idx_feedbacks_status ON public.feedbacks(status);
CREATE INDEX IF NOT EXISTS idx_feedbacks_org_id ON public.feedbacks(sender_org_id);
CREATE INDEX IF NOT EXISTS idx_feedbacks_created_at ON public.feedbacks(created_at);

-- Comments
COMMENT ON TABLE public.feedbacks IS 'Góp ý và phản hồi từ người dùng';
COMMENT ON COLUMN public.feedbacks.id IS 'UUID của feedback';
COMMENT ON COLUMN public.feedbacks.subject IS 'Chủ đề của góp ý';
COMMENT ON COLUMN public.feedbacks.content IS 'Nội dung chi tiết của góp ý';
COMMENT ON COLUMN public.feedbacks.is_private IS 'Cờ đánh dấu feedback có riêng tư hay không';
COMMENT ON COLUMN public.feedbacks.status IS 'Trạng thái: PENDING/PROCESSING/RESOLVED/REJECTED';
COMMENT ON COLUMN public.feedbacks.response_content IS 'Nội dung phản hồi từ admin';
COMMENT ON COLUMN public.feedbacks.response_date IS 'Thời gian phản hồi';
COMMENT ON COLUMN public.feedbacks.sender_id IS 'UUID của người gửi (tham chiếu đến users.id)';
COMMENT ON COLUMN public.feedbacks.sender_org_id IS 'UUID của tổ chức người gửi (tham chiếu đến organizations.id)';
COMMENT ON COLUMN public.feedbacks.responder_id IS 'UUID của người trả lời (tham chiếu đến users.id)';
COMMENT ON COLUMN public.feedbacks.responder_org_id IS 'UUID của tổ chức người trả lời (tham chiếu đến organizations.id)';
