package ai.dto.outer.otp.response;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OtpAuthResponseDto {
    int id;
    String userId;
    String customerCode;
    String organization;
    Boolean enable;
    Long dateCreated;
    Long dateModified;
    Boolean isAdmin;
    Boolean enableSms;
    Boolean enableAppCode;
    String email;
    String fullName;
    Boolean enableOtpApp;
    Boolean required;
    Long lastLoginDate;
    String activeCode;
    String searchField;
    String domain;
    Boolean sendSmsActiveCode;
    Boolean sendEmailActiveCode;
    Boolean requiredPincode;
    Boolean requiredActiveCode;
}
