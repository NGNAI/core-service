-- System Health permissions (cho admin UI team khác gọi API kiểm tra trạng thái hệ thống)
INSERT INTO public."permission" (id, created_at, created_by, updated_at, updated_by, description, name, "action", code, resource, target_resource, scopes) VALUES
    ('a1b2c3d4-0002-4000-8000-000000000001'::uuid, now(), '1e6633fb-2654-4bd5-aa7d-51bb86418987'::uuid, now(), '1e6633fb-2654-4bd5-aa7d-51bb86418987'::uuid, 'Xem trạng thái hệ thống', 'Xem trạng thái hệ thống', 'READ', 'SYSTEM_HEALTH:READ', 'SYSTEM_HEALTH', NULL, '{ALL}')
    ON CONFLICT (id) DO NOTHING;

-- Assign SYSTEM_HEALTH:READ to ROOT role
INSERT INTO public.role_permissions (permission_id, role_id, "scope") VALUES
    ('a1b2c3d4-0002-4000-8000-000000000001'::uuid, '80a432e5-455a-4232-9a90-9da219a0a543'::uuid, 'ALL')
    ON CONFLICT (permission_id, role_id) DO NOTHING;