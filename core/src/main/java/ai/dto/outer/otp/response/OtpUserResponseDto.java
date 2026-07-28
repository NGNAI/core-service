package ai.dto.outer.otp.response;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * DTO đại diện cho thông tin user LDAP từ OTP Service.
 * Dùng chung cho cả API auth, getUserDetail (single object) và searchUsers (array).
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OtpUserResponseDto {
    Integer id;
    String userId;
    String customerCode;
    String phone1;
    String phone2;
    String organization;
    String code;
    Boolean enable;
    String manualCode;
    Long dateCreated;
    Long dateModified;
    Boolean isAdmin;
    Boolean enableSms;
    Boolean enableAppCode;
    String email;
    String jobTitle;
    String cccd;
    String fullName;
    Boolean enableOtpApp;
    Boolean required;
    Long lastLoginDate;
    Long logonDuration;
    String activeCode;
    String searchField;
    String domain;
    String uid;
}