-- Organization table
INSERT INTO public.organizations (id,created_at,created_by,updated_at,updated_by,description,name,"path",parent_id) VALUES
	 ('1222cf1d-7443-4fc9-ba39-88c2812d3558'::uuid,'2026-03-24 15:41:17.741',NULL,'2026-03-24 15:41:17.741',NULL,'Root organization','Root','1222cf1d-7443-4fc9-ba39-88c2812d3558',NULL)
     ON CONFLICT (id) DO NOTHING;