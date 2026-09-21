-- V36: Thêm cấu hình cho tác vụ gợi ý câu hỏi (ask-autocomplete) — nhóm AI,
-- is_public=false.
--
-- Bối cảnh: gợi ý câu hỏi được sinh qua chính đường ống chung của các tác vụ AI phụ trợ
-- (RagService -> POST /generate/v1/chat/completions_simple), KHÔNG dùng client LLM riêng.
-- Toàn bộ ngưỡng nghiệp vụ của autocomplete đưa lên System Settings để chỉnh khi vận hành
-- mà không phải deploy lại.
--
-- Điểm quan trọng — chọn model:
--   * ai.suggestion.model để RỖNG nghĩa là dùng chung ai.model.
--   * Khi RAG service bổ sung model nhỏ/nhanh hơn, chỉ cần điền tên model vào setting này
--     để autocomplete dùng model đó mà không ảnh hưởng model của chat. Đây là lý do chính
--     các setting này nằm ở DB thay vì YAML.
--
-- Quy ước chung của repo: giá trị rỗng, <= 0 hoặc không parse được → dùng default trong code
-- (xem ai/constant/AiSuggestionConfig.java).
INSERT INTO public.system_settings (id, created_at, created_by, updated_at, updated_by, description, display_order, is_active, is_public, setting_group, setting_key, setting_type, setting_value) VALUES
    ('a0000009-0000-4000-8000-000000000001'::uuid, now(), NULL, now(), NULL, 'Model riêng cho tác vụ gợi ý câu hỏi (rỗng = dùng chung ai.model)', 9, true, false, 'AI', 'ai.suggestion.model', 'STRING', ''),
    ('a0000009-0000-4000-8000-000000000002'::uuid, now(), NULL, now(), NULL, 'Timeout (giây) cho lời gọi sinh gợi ý câu hỏi (người dùng đang gõ nên không thể chờ lâu)', 10, true, false, 'AI', 'ai.suggestion.timeoutSeconds', 'INTEGER', '15'),
    ('a0000009-0000-4000-8000-000000000003'::uuid, now(), NULL, now(), NULL, 'Số gợi ý câu hỏi tối đa trả về cho ô chat', 11, true, false, 'AI', 'ai.suggestion.maxSuggestions', 'INTEGER', '5'),
    ('a0000009-0000-4000-8000-000000000004'::uuid, now(), NULL, now(), NULL, 'Độ dài prefix tối thiểu để gọi model sinh gợi ý; ngắn hơn thì trả rỗng và không gọi', 12, true, false, 'AI', 'ai.suggestion.minPrefixChars', 'INTEGER', '3'),
    ('a0000009-0000-4000-8000-000000000005'::uuid, now(), NULL, now(), NULL, 'Độ dài prefix tối đa đưa vào prompt sinh gợi ý (dài hơn thì cắt lấy phần đuôi)', 13, true, false, 'AI', 'ai.suggestion.maxPrefixChars', 'INTEGER', '200'),
    ('a0000009-0000-4000-8000-000000000006'::uuid, now(), NULL, now(), NULL, 'Độ dài tối đa (ký tự) của mỗi gợi ý câu hỏi', 14, true, false, 'AI', 'ai.suggestion.maxSuggestionChars', 'INTEGER', '120'),
    ('a0000009-0000-4000-8000-000000000007'::uuid, now(), NULL, now(), NULL, 'Số request sinh gợi ý đồng thời tối đa; hết lượt thì trả rỗng ngay (chống dồn request)', 15, true, false, 'AI', 'ai.suggestion.maxConcurrentRequests', 'INTEGER', '4')
ON CONFLICT (setting_key) DO NOTHING;
