-- Default organization (dùng cho user LDAP mới chưa được admin gán vào tổ chức)
-- ID phải match với security.ldap.default-org-id trong application.yml
INSERT INTO public.organizations (id,created_at,created_by,updated_at,updated_by,description,name,"path",parent_id) VALUES
	 ('a1b2c3d4-e5f6-7890-abcd-ef1234567890'::uuid,'2026-03-24 15:41:17.741',NULL,'2026-03-24 15:41:17.741',NULL,'Default organization - tự động gán cho user LDAP mới','Default','1222cf1d-7443-4fc9-ba39-88c2812d3558/a1b2c3d4-e5f6-7890-abcd-ef1234567890','1222cf1d-7443-4fc9-ba39-88c2812d3558'::uuid)
     ON CONFLICT (id) DO NOTHING;
