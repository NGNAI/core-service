-- System Setting permissions
INSERT INTO public."permission" (id, created_at, created_by, updated_at, updated_by, description, name, "action", code, resource, target_resource, scopes) VALUES
    ('2aebea00-0168-4295-9dd4-4c31039973de'::uuid, now(), '1e6633fb-2654-4bd5-aa7d-51bb86418987', now(), '1e6633fb-2654-4bd5-aa7d-51bb86418987', 'Xem cấu hình hệ thống', 'Xem cấu hình', 'READ', 'SYSTEM_SETTING:READ', 'SYSTEM_SETTING', NULL, '{ALL}'),
    ('7fed9941-32b6-4788-b614-ff6df0caaf2a'::uuid, now(), '1e6633fb-2654-4bd5-aa7d-51bb86418987', now(), '1e6633fb-2654-4bd5-aa7d-51bb86418987', 'Thêm cấu hình hệ thống', 'Thêm cấu hình', 'CREATE', 'SYSTEM_SETTING:CREATE', 'SYSTEM_SETTING', NULL, '{ALL}'),
    ('82269fa3-e1a2-45f2-90a6-b29b0695d096'::uuid, now(), '1e6633fb-2654-4bd5-aa7d-51bb86418987', now(), '1e6633fb-2654-4bd5-aa7d-51bb86418987', 'Cập nhật cấu hình hệ thống', 'Cập nhật cấu hình', 'UPDATE', 'SYSTEM_SETTING:UPDATE', 'SYSTEM_SETTING', NULL, '{ALL}'),
    ('806f4b50-3b27-4ccc-872a-193874d0f10d'::uuid, now(), '1e6633fb-2654-4bd5-aa7d-51bb86418987', now(), '1e6633fb-2654-4bd5-aa7d-51bb86418987', 'Xoá cấu hình hệ thống', 'Xoá cấu hình', 'DELETE', 'SYSTEM_SETTING:DELETE', 'SYSTEM_SETTING', NULL, '{ALL}')
ON CONFLICT (id) DO NOTHING;
