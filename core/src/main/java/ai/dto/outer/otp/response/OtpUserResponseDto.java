package ai.dto.outer.otp.response;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * DTO đại diện cho thông tin user LDAP lấy từ OTP Service (API listing/search/detail).
 * Khác với {@link OtpAuthResponseDto} - DTO dành riêng cho response auth.
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OtpUserResponseDto {
    String userId;
    String fullName;
    String email;
    String phoneNumber;
    String organization;
    String domain;
    Boolean enable;
    Long dateCreated;
    Long dateModified;
    Long lastLoginDate;
}