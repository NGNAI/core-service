-- System Setting permissions
INSERT INTO public."permission" (id, created_at, created_by, updated_at, updated_by, description, name, "action", code, resource, target_resource, scopes) VALUES
    ('b0000001-0000-4000-8000-000000000001'::uuid, now(), NULL, now(), NULL, 'Xem cấu hình hệ thống', 'Xem cấu hình', 'READ', 'SYSTEM_SETTING:READ', 'SYSTEM_SETTING', NULL, '{ALL}'),
    ('b0000001-0000-4000-8000-000000000002'::uuid, now(), NULL, now(), NULL, 'Thêm cấu hình hệ thống', 'Thêm cấu hình', 'CREATE', 'SYSTEM_SETTING:CREATE', 'SYSTEM_SETTING', NULL, '{ALL}'),
    ('b0000001-0000-4000-8000-000000000003'::uuid, now(), NULL, now(), NULL, 'Cập nhật cấu hình hệ thống', 'Cập nhật cấu hình', 'UPDATE', 'SYSTEM_SETTING:UPDATE', 'SYSTEM_SETTING', NULL, '{ALL}'),
    ('b0000001-0000-4000-8000-000000000004'::uuid, now(), NULL, now(), NULL, 'Xoá cấu hình hệ thống', 'Xoá cấu hình', 'DELETE', 'SYSTEM_SETTING:DELETE', 'SYSTEM_SETTING', NULL, '{ALL}')
ON CONFLICT (id) DO NOTHING;
