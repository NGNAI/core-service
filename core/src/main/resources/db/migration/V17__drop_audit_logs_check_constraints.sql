-- V17: Drop CHECK constraints tự sinh bởi Hibernate trên bảng audit_logs
-- 
-- Lý do: Hibernate ddl-auto=update tự sinh CHECK constraint cho cột enum
-- (audit_logs_action_check, audit_logs_resource_check, audit_logs_status_check)
-- nhưng KHÔNG tự cập nhật khi thêm giá trị enum mới trong Java.
-- Việc maintain constraint đồng bộ với Java enum là không bền vững.
-- Source of truth là Java enum — DB không cần lớp bảo vệ này.

ALTER TABLE IF EXISTS public.audit_logs DROP CONSTRAINT IF EXISTS audit_logs_action_check;
ALTER TABLE IF EXISTS public.audit_logs DROP CONSTRAINT IF EXISTS audit_logs_resource_check;
ALTER TABLE IF EXISTS public.audit_logs DROP CONSTRAINT IF EXISTS audit_logs_status_check;
