-- Thêm cột reasoning_steps cho bảng message
-- reasoning_steps: chuỗi JSON array chứa các bước suy luận (reasoning steps) của AI khi trả lời, null = chưa có
ALTER TABLE public.message
    ADD COLUMN IF NOT EXISTS reasoning_steps text;
