package ai.dto.own.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthRequestDto {
    @NotBlank(message = "USERNAME_CAN_NOT_BE_NULL_OR_EMPTY")
    String username;
    @NotBlank(message = "PASSWORD_CAN_NOT_BE_NULL_OR_EMPTY")
    String password;
    @NotBlank(message = "SOURCE_CAN_NOT_BE_NULL_OR_EMPTY")
    String source;
}
