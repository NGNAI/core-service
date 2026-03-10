package ai.dto.own.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreateRequestDto {
    @NotBlank(message = "USER_NAME_CAN_NOT_BE_NULL_OR_EMPTY")
    String userName;
    @NotBlank(message = "USER_FIRST_NAME_CAN_NOT_BE_NULL_OR_EMPTY")
    String firstName;
    String lastName;
    @NotBlank(message = "USER_PASSWORD_CAN_NOT_BE_NULL_OR_EMPTY")
    String password;
    @Min(value = 0, message = "USER_GENDER_VALUE_INVALID")
    @Max(value = 1, message = "USER_GENDER_VALUE_INVALID")
    int gender;
    @Email(message = "USER_EMAIL_VALUE_INVALID")
    String email;
    String phoneNumber;
    String source;
}
