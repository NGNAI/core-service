-- User table
INSERT INTO public.users (id,active,created_at,created_by,updated_at,updated_by,email,first_name,gender,last_login,last_name,"password",phone_number,"source",user_name) VALUES
	 ('1e6633fb-2654-4bd5-aa7d-51bb86418988'::uuid,true,'2026-03-24 15:41:17.972',NULL,'2026-04-21 15:51:15.436','1e6633fb-2654-4bd5-aa7d-51bb86418987'::uuid,'root@ngn.com.vn','Root',1,'2026-04-21 15:51:15.433','','$2a$10$OA3RoMsYI.ckNxbYTc5tuOukOWGgfG8/Eh4u/A/R3HG7zh5j2haOm','0333282828','local','root')
     ON CONFLICT (id) DO NOTHING;