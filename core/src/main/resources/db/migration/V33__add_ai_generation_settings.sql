-- V33: Thêm cấu hình cho các tác vụ sinh văn bản bằng AI (nhóm AI, is_public=false).
--
-- Mục đích: trước đây các ngưỡng cắt ngắn input / giới hạn độ dài output của tác vụ
-- sinh tiêu đề và nén rolling summary bị hard-code trong RagService, không chỉnh được
-- khi vận hành. Với các model nhỏ (gpt-oss-20b) các ngưỡng này cần tinh chỉnh theo
-- thực tế tài liệu, nên đưa lên System Settings.
--
-- Quy ước: giá trị <= 0 hoặc không parse được → dùng default trong code.
INSERT INTO public.system_settings (id, created_at, created_by, updated_at, updated_by, description, display_order, is_active, is_public, setting_group, setting_key, setting_type, setting_value) VALUES
    ('a0000008-0000-4000-8000-000000000001'::uuid, now(), NULL, now(), NULL, 'Ngân sách ký tự tối đa của input khi sinh tiêu đề (note/topic)', 4, true, false, 'AI', 'ai.title.maxInputChars', 'INTEGER', '2000'),
    ('a0000008-0000-4000-8000-000000000002'::uuid, now(), NULL, now(), NULL, 'Ngân sách ký tự tối đa của hội thoại đưa vào prompt nén summary', 5, true, false, 'AI', 'ai.summary.maxInputChars', 'INTEGER', '12000'),
    ('a0000008-0000-4000-8000-000000000003'::uuid, now(), NULL, now(), NULL, 'Số từ tối đa của rolling summary đầu ra', 6, true, false, 'AI', 'ai.summary.maxWords', 'INTEGER', '300'),
    ('a0000008-0000-4000-8000-000000000004'::uuid, now(), NULL, now(), NULL, 'Số lần thử sinh summary source-guide tối đa trước khi đánh dấu FAILED (0 = dùng mặc định 5)', 7, true, false, 'AI', 'ai.sourceGuide.maxRetryAttempts', 'INTEGER', '5'),
    ('a0000008-0000-4000-8000-000000000005'::uuid, now(), NULL, now(), NULL, 'Chỉ dẫn sinh summary gửi kèm khi trigger source-guide (rỗng = dùng mặc định trong code)', 8, true, false, 'AI', 'ai.sourceGuide.generationInstruction', 'STRING', '')
ON CONFLICT (setting_key) DO NOTHING;
