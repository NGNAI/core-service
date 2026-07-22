-- V15: Xóa SYSTEM_SETTING & SYSTEM_HEALTH khỏi hệ thống RBAC
-- Các resource này giờ được bảo vệ bằng danh sách username hardcode (security.admin-allowed-usernames)
-- thay vì qua Role/Permission, để không hiển thị trong quản lý Role trên Admin UI.

-- 1. Xóa gán quyền (role_permissions) cho các permission thuộc 2 resource này
DELETE FROM public.role_permissions
WHERE permission_id IN (
    SELECT id FROM public."permission" WHERE resource IN ('SYSTEM_SETTING', 'SYSTEM_HEALTH')
);

-- 2. Xóa các permission rows thuộc 2 resource này
DELETE FROM public."permission"
WHERE resource IN ('SYSTEM_SETTING', 'SYSTEM_HEALTH');