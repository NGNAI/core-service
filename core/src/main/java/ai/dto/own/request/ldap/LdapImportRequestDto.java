package ai.dto.own.request.ldap;

import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO để import một loạt user LDAP vào hệ thống.
 * Admin search user LDAP → chọn nhiều user → chọn org → import 1 lần.
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LdapImportRequestDto {
    /**
     * Danh sách userId trong LDAP (lấy từ OTP Service).
     */
    @NotEmpty(message = "Danh sách user ID không được để trống")
    List<String> ldapUserIds;

    /**
     * Organization đích để gán user vào.
     */
    UUID organizationId;

    /**
     * Role gán cho user trong org.
     * Để null → fallback về role có defaultAssign=true.
     */
    UUID roleId;
}