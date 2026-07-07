-- System Settings table
CREATE TABLE IF NOT EXISTS public.system_settings (
    id uuid NOT NULL,
    created_at timestamp(6) with time zone,
    created_by uuid,
    updated_at timestamp(6) with time zone,
    updated_by uuid,
    description varchar(512),
    display_order int4 DEFAULT 0,
    is_active bool NOT NULL DEFAULT true,
    is_public bool NOT NULL DEFAULT false,
    setting_group varchar(64) NOT NULL DEFAULT 'GENERAL',
    setting_key varchar(128) NOT NULL,
    setting_type varchar(32) NOT NULL DEFAULT 'STRING',
    setting_value text,
    CONSTRAINT uk_system_settings_key UNIQUE (setting_key),
    CONSTRAINT system_settings_pkey PRIMARY KEY (id)
);

-- Seed default system settings
INSERT INTO public.system_settings (id, created_at, created_by, updated_at, updated_by, description, display_order, is_active, is_public, setting_group, setting_key, setting_type, setting_value) VALUES
    -- GENERAL
    ('a0000001-0000-4000-8000-000000000001'::uuid, now(), NULL, now(), NULL, 'Tên hệ thống', 1, true, true, 'GENERAL', 'system.name', 'STRING', 'NGN AI'),
    ('a0000001-0000-4000-8000-000000000002'::uuid, now(), NULL, now(), NULL, 'Mô tả hệ thống', 2, true, true, 'GENERAL', 'system.description', 'STRING', 'Hệ thống trí tuệ nhân tạo NGN'),
    ('a0000001-0000-4000-8000-000000000003'::uuid, now(), NULL, now(), NULL, 'URL logo hệ thống', 3, true, true, 'GENERAL', 'system.logo', 'STRING', ''),
    ('a0000001-0000-4000-8000-000000000004'::uuid, now(), NULL, now(), NULL, 'URL favicon', 4, true, true, 'GENERAL', 'system.favicon', 'STRING', ''),
    ('a0000001-0000-4000-8000-000000000005'::uuid, now(), NULL, now(), NULL, 'Ngôn ngữ mặc định', 5, true, true, 'GENERAL', 'system.language', 'STRING', 'vi'),
    ('a0000001-0000-4000-8000-000000000006'::uuid, now(), NULL, now(), NULL, 'Múi giờ', 6, true, true, 'GENERAL', 'system.timezone', 'STRING', 'Asia/Ho_Chi_Minh'),
    ('a0000001-0000-4000-8000-000000000007'::uuid, now(), NULL, now(), NULL, 'Bản quyền', 7, true, true, 'GENERAL', 'system.copyright', 'STRING', '© 2026 NGN AI. All rights reserved.'),
    ('a0000001-0000-4000-8000-000000000008'::uuid, now(), NULL, now(), NULL, 'Phiên bản hệ thống', 8, true, true, 'GENERAL', 'system.version', 'STRING', '1.0.0'),

    -- CONTACT
    ('a0000002-0000-4000-8000-000000000001'::uuid, now(), NULL, now(), NULL, 'Email liên hệ', 1, true, true, 'CONTACT', 'contact.email', 'STRING', 'contact@ngn.ai'),
    ('a0000002-0000-4000-8000-000000000002'::uuid, now(), NULL, now(), NULL, 'Số điện thoại', 2, true, true, 'CONTACT', 'contact.phone', 'STRING', '1900xxxx'),
    ('a0000002-0000-4000-8000-000000000003'::uuid, now(), NULL, now(), NULL, 'Hotline hỗ trợ', 3, true, true, 'CONTACT', 'contact.hotline', 'STRING', '1900xxxx'),
    ('a0000002-0000-4000-8000-000000000004'::uuid, now(), NULL, now(), NULL, 'Địa chỉ văn phòng', 4, true, true, 'CONTACT', 'contact.address', 'STRING', 'Hà Nội, Việt Nam'),
    ('a0000002-0000-4000-8000-000000000005'::uuid, now(), NULL, now(), NULL, 'Website', 5, true, true, 'CONTACT', 'contact.website', 'STRING', 'https://ngn.ai'),

    -- SOCIAL
    ('a0000003-0000-4000-8000-000000000001'::uuid, now(), NULL, now(), NULL, 'Facebook', 1, true, true, 'SOCIAL', 'social.facebook', 'STRING', ''),
    ('a0000003-0000-4000-8000-000000000002'::uuid, now(), NULL, now(), NULL, 'Youtube', 2, true, true, 'SOCIAL', 'social.youtube', 'STRING', ''),
    ('a0000003-0000-4000-8000-000000000003'::uuid, now(), NULL, now(), NULL, 'LinkedIn', 3, true, true, 'SOCIAL', 'social.linkedin', 'STRING', ''),
    ('a0000003-0000-4000-8000-000000000004'::uuid, now(), NULL, now(), NULL, 'Zalo', 4, true, true, 'SOCIAL', 'social.zalo', 'STRING', ''),

    -- SECURITY
    ('a0000004-0000-4000-8000-000000000001'::uuid, now(), NULL, now(), NULL, 'Số lần đăng nhập sai tối đa', 1, true, false, 'SECURITY', 'security.maxLoginAttempts', 'INTEGER', '5'),
    ('a0000004-0000-4000-8000-000000000002'::uuid, now(), NULL, now(), NULL, 'Thời gian hết phiên (phút)', 2, true, false, 'SECURITY', 'security.sessionTimeout', 'INTEGER', '60'),
    ('a0000004-0000-4000-8000-000000000003'::uuid, now(), NULL, now(), NULL, 'Độ dài mật khẩu tối thiểu', 3, true, false, 'SECURITY', 'security.passwordMinLength', 'INTEGER', '8'),
    ('a0000004-0000-4000-8000-000000000004'::uuid, now(), NULL, now(), NULL, 'Bật xác thực hai yếu tố', 4, true, false, 'SECURITY', 'security.twoFactorEnabled', 'BOOLEAN', 'false'),

    -- AI
    ('a0000005-0000-4000-8000-000000000001'::uuid, now(), NULL, now(), NULL, 'Model AI mặc định', 1, true, false, 'AI', 'ai.model', 'STRING', 'gpt-4'),
    ('a0000005-0000-4000-8000-000000000002'::uuid, now(), NULL, now(), NULL, 'Nhiệt độ sinh (temperature)', 2, true, false, 'AI', 'ai.temperature', 'DOUBLE', '0.7'),
    ('a0000005-0000-4000-8000-000000000003'::uuid, now(), NULL, now(), NULL, 'Số token tối đa mỗi request', 3, true, false, 'AI', 'ai.maxTokens', 'INTEGER', '2048'),

    -- SYSTEM
    ('a0000006-0000-4000-8000-000000000001'::uuid, now(), NULL, now(), NULL, 'Chế độ bảo trì', 1, true, false, 'SYSTEM', 'system.maintenanceMode', 'BOOLEAN', 'false'),
    ('a0000006-0000-4000-8000-000000000002'::uuid, now(), NULL, now(), NULL, 'Kích thước file tối đa (MB)', 2, true, false, 'SYSTEM', 'system.maxFileSize', 'INTEGER', '100'),
    ('a0000006-0000-4000-8000-000000000003'::uuid, now(), NULL, now(), NULL, 'Các loại file được phép tải lên', 3, true, false, 'SYSTEM', 'system.allowedFileTypes', 'STRING', 'pdf,doc,docx,txt,png,jpg,jpeg')
ON CONFLICT (setting_key) DO NOTHING;
