package ai.dto.own.request;

import ai.constant.InputValidateKey;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserPasswordUpdateRequestDto {
    @NotBlank(message = InputValidateKey.USER_PASSWORD_CAN_NOT_BE_NULL_OR_EMPTY)
    String oldPassword;
    @NotBlank(message = InputValidateKey.USER_PASSWORD_CAN_NOT_BE_NULL_OR_EMPTY)
    @Min(value = 5, message = InputValidateKey.INVALID_PASSWORD)
    String newPassword;
}
