-- Assign SYSTEM_SETTING permissions to ROOT role
INSERT INTO public.role_permissions (permission_id, role_id, "scope") VALUES
    ('b0000001-0000-4000-8000-000000000001'::uuid, '80a432e5-455a-4232-9a90-9da219a0a543'::uuid, 'ALL'),
    ('b0000001-0000-4000-8000-000000000002'::uuid, '80a432e5-455a-4232-9a90-9da219a0a543'::uuid, 'ALL'),
    ('b0000001-0000-4000-8000-000000000003'::uuid, '80a432e5-455a-4232-9a90-9da219a0a543'::uuid, 'ALL'),
    ('b0000001-0000-4000-8000-000000000004'::uuid, '80a432e5-455a-4232-9a90-9da219a0a543'::uuid, 'ALL')
ON CONFLICT (permission_id, role_id) DO NOTHING;
