-- V22: Tạo bảng prompt_templates cho tính năng Quick Prompt Template
-- Mục đích: lưu các prompt/input nhanh thường dùng khi chat với Topic / NotebookLM
--   - scope = SYSTEM: do admin tạo, dùng chung cho tất cả org (global)
--   - scope = USER  : do người dùng tự tạo, gắn với owner + organization
--   - prompt_type   : TOPIC / NOTEBOOK / BOTH (target đối tượng chatbot)
-- Seed data: 15 mẫu system prompt (5 TOPIC, 5 NOTEBOOK, 5 BOTH) để luôn có mẫu khi deploy

CREATE TABLE IF NOT EXISTS public.prompt_templates (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title           VARCHAR(256) NOT NULL,
    content         TEXT         NOT NULL,
    prompt_type     VARCHAR(32)  NOT NULL,
    scope           VARCHAR(32)  NOT NULL,
    display_order   INTEGER      NOT NULL DEFAULT 0,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    owner_id        UUID,
    organization_id UUID,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    created_by      UUID,
    updated_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_by      UUID
);

-- Index cho việc lọc theo scope / prompt_type / owner / org
CREATE INDEX IF NOT EXISTS idx_prompt_templates_scope ON public.prompt_templates (scope);
CREATE INDEX IF NOT EXISTS idx_prompt_templates_type  ON public.prompt_templates (prompt_type);
CREATE INDEX IF NOT EXISTS idx_prompt_templates_owner ON public.prompt_templates (owner_id);
CREATE INDEX IF NOT EXISTS idx_prompt_templates_org   ON public.prompt_templates (organization_id);

-- Comment cho documentation
COMMENT ON TABLE  public.prompt_templates IS 'Prompt template cho chat với Topic / NotebookLM (SYSTEM global + USER cá nhân)';
COMMENT ON COLUMN public.prompt_templates.prompt_type IS 'Loại chatbot: TOPIC / NOTEBOOK / BOTH';
COMMENT ON COLUMN public.prompt_templates.scope IS 'Phạm vi: SYSTEM (admin tạo, global) / USER (người dùng tự tạo)';
COMMENT ON COLUMN public.prompt_templates.owner_id IS 'Chủ sở hữu (chỉ dùng cho scope=USER), NULL với SYSTEM';
COMMENT ON COLUMN public.prompt_templates.organization_id IS 'Org của prompt user, NULL với SYSTEM (global)';

-- ========================================================================
-- Seed data: system prompts (scope = SYSTEM, global)
-- Dùng UUID cố định + ON CONFLICT DO NOTHING để idempotent
-- ========================================================================

-- TOPIC prompts
INSERT INTO public.prompt_templates (id, title, content, prompt_type, scope, display_order, is_active) VALUES
    ('11111111-1111-1111-1111-111111111101', 'Tóm tắt nội dung',        'Hãy tóm tắt nội dung chính của các tài liệu trong Topic này thành những ý ngắn gọn, dễ hiểu.', 'TOPIC', 'SYSTEM', 1, TRUE),
    ('11111111-1111-1111-1111-111111111102', 'Giải thích khái niệm',     'Giải thích khái niệm/quy trình đang được đề cập trong các tài liệu của Topic này một cách dễ hiểu.', 'TOPIC', 'SYSTEM', 2, TRUE),
    ('11111111-1111-1111-1111-111111111103', 'So sánh quan điểm',        'So sánh các quan điểm/nội dung giữa các tài liệu trong Topic này, chỉ ra điểm giống và khác nhau.', 'TOPIC', 'SYSTEM', 3, TRUE),
    ('11111111-1111-1111-1111-111111111104', 'Trả lời có trích dẫn',     'Trả lời câu hỏi của tôi dựa trên tài liệu trong Topic, kèm trích dẫn cụ thể (tên tài liệu, vị trí).', 'TOPIC', 'SYSTEM', 4, TRUE),
    ('11111111-1111-1111-1111-111111111105', 'Key takeaways',            'Liệt kê tất cả các điểm chính/key takeaways quan trọng từ các tài liệu trong Topic này.', 'TOPIC', 'SYSTEM', 5, TRUE)
ON CONFLICT (id) DO NOTHING;

-- NOTEBOOK prompts
INSERT INTO public.prompt_templates (id, title, content, prompt_type, scope, display_order, is_active) VALUES
    ('11111111-1111-1111-1111-111111111201', 'Ôn tập bằng câu hỏi',      'Giúp tôi ôn tập nội dung trong Notebook này bằng cách tạo các câu hỏi trắc nghiệm kèm đáp án.', 'NOTEBOOK', 'SYSTEM', 1, TRUE),
    ('11111111-1111-1111-1111-111111111202', 'Phân tích chuyên sâu',     'Phân tích sâu nội dung trong các nguồn của Notebook, chỉ ra những thông tin quan trọng và ý nghĩa của chúng.', 'NOTEBOOK', 'SYSTEM', 2, TRUE),
    ('11111111-1111-1111-1111-111111111203', 'Lập dàn ý',                'Dựa trên nội dung trong Notebook, lập dàn ý chi tiết cho một bài viết/bài thuyết trình.', 'NOTEBOOK', 'SYSTEM', 3, TRUE),
    ('11111111-1111-1111-1111-111111111204', 'Tổng hợp kiến thức',       'Tổng hợp toàn bộ kiến thức trong Notebook thành một bản tóm tắt có cấu trúc rõ ràng.', 'NOTEBOOK', 'SYSTEM', 4, TRUE),
    ('11111111-1111-1111-1111-111111111205', 'Hỏi đáp có nguồn',         'Trả lời câu hỏi của tôi dựa trên các nguồn trong Notebook, kèm trích dẫn nguồn chính xác.', 'NOTEBOOK', 'SYSTEM', 5, TRUE)
ON CONFLICT (id) DO NOTHING;

-- BOTH prompts
INSERT INTO public.prompt_templates (id, title, content, prompt_type, scope, display_order, is_active) VALUES
    ('11111111-1111-1111-1111-111111111301', 'Làm quen nội dung',        'Xin chào! Hãy làm quen với nội dung tôi đã cung cấp và sẵn sàng trả lời câu hỏi của tôi.', 'BOTH', 'SYSTEM', 1, TRUE),
    ('11111111-1111-1111-1111-111111111302', 'Giải thích đơn giản',      'Giải thích nội dung chính bằng ngôn ngữ đơn giản, dễ hiểu, phù hợp cho người mới bắt đầu.', 'BOTH', 'SYSTEM', 2, TRUE),
    ('11111111-1111-1111-1111-111111111303', 'Ví dụ minh họa',           'Đưa ra các ví dụ minh họa thực tế cho các khái niệm được đề cập trong tài liệu.', 'BOTH', 'SYSTEM', 3, TRUE),
    ('11111111-1111-1111-1111-111111111304', 'Phát hiện mâu thuẫn',      'Kiểm tra và chỉ ra những điểm mâu thuẫn hoặc thiếu nhất quán giữa các tài liệu.', 'BOTH', 'SYSTEM', 4, TRUE),
    ('11111111-1111-1111-1111-111111111305', 'Gợi ý câu hỏi tiếp theo', 'Dựa trên nội dung tài liệu, gợi ý những câu hỏi tiếp theo mà tôi có thể hỏi.', 'BOTH', 'SYSTEM', 5, TRUE)
ON CONFLICT (id) DO NOTHING;
