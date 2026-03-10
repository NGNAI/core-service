package ai.dto.own.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserUpdateRequestDto {
    @NotBlank(message = "USER_FIRST_NAME_CAN_NOT_BE_NULL_OR_EMPTY")
    String firstName;
    String lastName;
    @Min(value = 0, message = "USER_GENDER_VALUE_INVALID")
    @Max(value = 1, message = "USER_GENDER_VALUE_INVALID")
    int gender;
    @Email(message = "USER_EMAIL_VALUE_INVALID")
    String email;
    String phoneNumber;
}
