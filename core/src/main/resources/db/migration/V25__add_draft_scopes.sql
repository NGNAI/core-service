-- Thêm cột scopes vào bảng draft để lưu phạm vi dữ liệu (RAG scope: global/local/personal) chọn lúc tạo draft.
-- Dạng text[] giống cột scopes của bảng permission (do Hibernate @JdbcTypeCode(SqlTypes.ARRAY) quản lý).
-- Bảng draft do Hibernate ddl-auto=update tự sinh nên trên DB mới Flyway chạy trước khi bảng tồn tại → phải guarded
-- (nếu bảng chưa có thì bỏ qua, Hibernate sẽ tạo cột theo annotation).
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_name = 'draft'
    ) THEN
        ALTER TABLE draft ADD COLUMN IF NOT EXISTS scopes text[];
    END IF;
END $$;
