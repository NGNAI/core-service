-- Assign SYSTEM_SETTING permissions to ROOT role
INSERT INTO public.role_permissions (permission_id, role_id, "scope") VALUES
    ('2aebea00-0168-4295-9dd4-4c31039973de'::uuid, '80a432e5-455a-4232-9a90-9da219a0a543'::uuid, 'ALL'),
    ('7fed9941-32b6-4788-b614-ff6df0caaf2a'::uuid, '80a432e5-455a-4232-9a90-9da219a0a543'::uuid, 'ALL'),
    ('82269fa3-e1a2-45f2-90a6-b29b0695d096'::uuid, '80a432e5-455a-4232-9a90-9da219a0a543'::uuid, 'ALL'),
    ('806f4b50-3b27-4ccc-872a-193874d0f10d'::uuid, '80a432e5-455a-4232-9a90-9da219a0a543'::uuid, 'ALL')
ON CONFLICT (permission_id, role_id) DO NOTHING;
