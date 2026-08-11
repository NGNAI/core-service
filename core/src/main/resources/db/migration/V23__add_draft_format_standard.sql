-- Thêm cột format_standard vào bảng draft để lưu chuẩn định dạng văn bản (format) chọn lúc tạo
ALTER TABLE draft ADD COLUMN IF NOT EXISTS format_standard VARCHAR(255);
