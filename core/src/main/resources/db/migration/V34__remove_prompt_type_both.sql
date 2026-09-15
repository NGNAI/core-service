-- V34: Loại bỏ hoàn toàn giá trị enum BOTH khỏi prompt_type
-- ============================================================================
-- Lý do: BOTH là giá trị "gộp" (dùng cho cả Topic lẫn Notebook) và sai nghĩa — nếu sau này
-- thêm loại chatbot mới (vd DRAFT / CHAT) thì BOTH không thể biểu diễn "tất cả các loại"
-- nữa (BOTH chỉ có nghĩa với đúng 2 giá trị). Ngoài ra logic filter trong code đang dùng
-- equal(prompt_type, :type) nên lọc TOPIC sẽ làm mất luôn các prompt BOTH.
--
-- Chiến lược (giữ nguyên nội dung, KHÔNG mất prompt):
--   Bước 1 — nhân bản mọi record prompt_type='BOTH' thành 1 bản 'NOTEBOOK'.
--   Bước 2 — đổi các record 'BOTH' gốc thành 'TOPIC'.
--   Kết quả: 1 record BOTH cũ -> 2 record (TOPIC + NOTEBOOK) cùng nội dung, cùng owner/org.
--
-- Thứ tự BẮT BUỘC: INSERT trước rồi mới UPDATE (nếu UPDATE trước, điều kiện WHERE prompt_type='BOTH'
-- sẽ không còn match record nào để nhân bản).
--
-- Idempotent: chạy lần 2 không còn record 'BOTH' -> INSERT 0 dòng, UPDATE 0 dòng.
-- Không thêm CHECK constraint trên prompt_type: theo quy ước V17, Java enum là source of truth.
-- ============================================================================

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_name = 'prompt_templates'
    ) THEN
        -- ────────────────────────────────────────────────────────────────────
        -- Bước 1: nhân bản BOTH -> NOTEBOOK (giữ nguyên toàn bộ nội dung/cấu hình)
        -- ────────────────────────────────────────────────────────────────────
        INSERT INTO public.prompt_templates (
            title, content, prompt_type, scope, display_order, is_active,
            owner_id, organization_id, created_at, created_by, updated_at, updated_by
        )
        SELECT
            title, content, 'NOTEBOOK', scope, display_order, is_active,
            owner_id, organization_id, now(), created_by, now(), updated_by
        FROM public.prompt_templates
        WHERE prompt_type = 'BOTH';

        -- ────────────────────────────────────────────────────────────────────
        -- Bước 2: đổi BOTH gốc -> TOPIC
        -- ────────────────────────────────────────────────────────────────────
        UPDATE public.prompt_templates
        SET prompt_type = 'TOPIC',
            updated_at  = now()
        WHERE prompt_type = 'BOTH';
    END IF;
END $$;

-- Cập nhật lại comment mô tả enum trên cột
COMMENT ON COLUMN public.prompt_templates.prompt_type IS 'Loại chatbot: TOPIC / NOTEBOOK (mỗi prompt 1 loại, dùng chung thì tạo nhiều record)';
