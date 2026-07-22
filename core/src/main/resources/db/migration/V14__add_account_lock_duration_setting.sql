-- Thêm setting cấu hình thời gian khoá tài khoản khi đăng nhập sai quá nhiều lần (phút)
-- Thuộc nhóm SECURITY, is_public=false (chỉ admin quản lý qua /admin/system-settings)
INSERT INTO public.system_settings (id, created_at, created_by, updated_at, updated_by, description, display_order, is_active, is_public, setting_group, setting_key, setting_type, setting_value) VALUES
    ('a0000004-0000-4000-8000-000000000005'::uuid, now(), NULL, now(), NULL, 'Thời gian khoá tài khoản khi đăng nhập sai quá nhiều lần (phút)', 5, true, false, 'SECURITY', 'security.accountLockDuration', 'INTEGER', '30')
ON CONFLICT (setting_key) DO NOTHING;