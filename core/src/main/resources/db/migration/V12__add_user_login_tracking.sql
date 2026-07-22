-- Thêm cột tracking đăng nhập cho bảng users
-- login_attempts: số lần đăng nhập sai liên tiếp
-- locked_until: thời gian khoá tài khoản đến khi (null = không bị khoá)
ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS login_attempts int4 NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS locked_until timestamp(6) with time zone;