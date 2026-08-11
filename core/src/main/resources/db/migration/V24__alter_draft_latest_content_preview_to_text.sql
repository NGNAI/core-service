-- Đổi cột latest_content_preview sang TEXT để lưu nội dung soạn thảo dài (vài trang A4)
-- Bảng draft do Hibernate ddl-auto=update tự sinh nên có thể tồn tại column dạng varchar(255);
-- ddl-auto=update không đổi type cột đã tồn tại nên cần migration này.
-- Guarded: nếu DB mới (chưa có cột) thì bỏ qua, Hibernate sẽ tạo cột TEXT theo annotation.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'draft' AND column_name = 'latest_content_preview'
    ) THEN
        ALTER TABLE draft ALTER COLUMN latest_content_preview TYPE TEXT;
    END IF;
END $$;
