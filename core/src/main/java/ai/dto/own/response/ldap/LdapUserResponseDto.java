package ai.dto.own.response.ldap;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

/**
 * Thông tin user LDAP từ OTP Service kèm trạng thái import trong hệ thống.
 */
@Data
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LdapUserResponseDto {
    String userId;
    String fullName;
    String email;
    String phoneNumber;
    String organization;
    String domain;
    Boolean enable;

    /**
     * Đã import vào hệ thống chưa (đã có UserEntity với source=ldap).
     */
    boolean imported;

    /**
     * ID trong DB nếu đã import.
     */
    UUID existingUserId;
}