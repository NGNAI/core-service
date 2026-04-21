package ai.dto.own.request;

import ai.annotation.EnumValue;
import ai.annotation.PhoneNumber;
import ai.constant.InputValidateKey;
import ai.enums.UserSource;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreateRequestDto {
    @NotBlank(message = InputValidateKey.USER_NAME_CAN_NOT_BE_NULL_OR_EMPTY)
    String userName;
    @NotBlank(message = InputValidateKey.USER_FIRST_NAME_CAN_NOT_BE_NULL_OR_EMPTY)
    String firstName;
    String lastName;
    @NotBlank(message = InputValidateKey.USER_PASSWORD_CAN_NOT_BE_NULL_OR_EMPTY)
    String password;
    @Min(value = 0, message = InputValidateKey.USER_GENDER_VALUE_INVALID)
    @Max(value = 1, message = InputValidateKey.USER_GENDER_VALUE_INVALID)
    int gender;
    @NotBlank(message = InputValidateKey.USER_EMAIL_CAN_NOT_BE_NULL_OR_EMPTY)
    @Email(message = InputValidateKey.USER_EMAIL_VALUE_INVALID)
    String email;
    @PhoneNumber(message = InputValidateKey.USER_PHONE_NUMBER_VALUE_INVALID)
    String phoneNumber;
    @Builder.Default
    boolean active = true;
    @NotBlank(message = InputValidateKey.USER_SOURCE_CAN_NOT_BE_NULL_OR_EMPTY)
    @EnumValue(enumClass = UserSource.class, message = InputValidateKey.INVALID_USER_SOURCE_VALUE)
    String source;
}
