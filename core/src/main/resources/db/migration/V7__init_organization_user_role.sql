-- Organization User Role table
INSERT INTO public.organization_user_role (user_id,organization_id,role_id) VALUES
	 ('1e6633fb-2654-4bd5-aa7d-51bb86418988'::uuid,'1222cf1d-7443-4fc9-ba39-88c2812d3558'::uuid,'80a432e5-455a-4232-9a90-9da219a0a543'::uuid)
	 ON CONFLICT (user_id,organization_id,role_id) DO NOTHING;
