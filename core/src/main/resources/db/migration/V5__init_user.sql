-- User table
-- MẬT KHẨU MẶC ĐỊNH cho tài khoản root: Admin@123  (đổi ngay sau lần đăng nhập đầu tiên!)
-- Mật khẩu được hash ngay tại thời điểm migration bằng pgcrypto crypt()/gen_salt('bf')
-- -> tạo ra hash bcrypt '$2a$...' tương thích với BCryptPasswordEncoder của Spring Security.
--
-- Lưu ý: cột login_attempts được Hibernate (ddl-auto=update) tạo là NOT NULL không có default,
-- nên seed phải cung cấp giá trị tường minh để chạy được trên DB mới hoàn toàn.

-- Cần extension pgcrypto cho hàm crypt()/gen_salt() (yêu cầu quyền superuser/owner DB)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO public.users (id,active,created_at,created_by,updated_at,updated_by,email,first_name,gender,last_login,last_name,"password",phone_number,"source",user_name,login_attempts) VALUES
	 ('1e6633fb-2654-4bd5-aa7d-51bb86418988'::uuid,true,'2026-03-24 15:41:17.972',NULL,'2026-04-21 15:51:15.436','1e6633fb-2654-4bd5-aa7d-51bb86418987'::uuid,'root@ngn.com.vn','Root',1,'2026-04-21 15:51:15.433','',crypt('P@ssw0rd!@#', gen_salt('bf')),'0333282828','local','root',0)
     ON CONFLICT (id) DO NOTHING;