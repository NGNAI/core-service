-- Role table
INSERT INTO public."role" (id,created_at,created_by,updated_at,updated_by,default_assign,description,name) VALUES
	 ('80a432e5-455a-4232-9a90-9da219a0a543'::uuid,now(),'1e6633fb-2654-4bd5-aa7d-51bb86418987'::uuid,now(),'1e6633fb-2654-4bd5-aa7d-51bb86418987'::uuid,false,'Quyền cho tài khoản Root','ROOT'),
	 ('a1b2c3d4-0003-4000-8000-000000000001'::uuid,now(),'1e6633fb-2654-4bd5-aa7d-51bb86418987'::uuid,now(),'1e6633fb-2654-4bd5-aa7d-51bb86418987'::uuid,true,'Vai trò mặc định cho người dùng','USER'),
	 ('a1b2c3d4-0004-4000-8000-000000000001'::uuid,now(),'1e6633fb-2654-4bd5-aa7d-51bb86418987'::uuid,now(),'1e6633fb-2654-4bd5-aa7d-51bb86418987'::uuid,false,'Quản lý đơn vị (org) của mình và các đơn vị con','ADMIN_ORG')
	 ON CONFLICT (id) DO NOTHING;