package ai.dto.own.request;

import ai.annotation.EnumValue;
import ai.constant.InputValidateKey;
import ai.enums.UserSource;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@JsonPropertyOrder({
        "username",
        "password",
        "source"
})
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthRequestDto {
    @Schema(description = "Username for authentication, e.g., email or unique username", example = "admin")
    @NotBlank(message = InputValidateKey.USERNAME_CAN_NOT_BE_NULL_OR_EMPTY)
    String username;

    @Schema(description = "Password for authentication. For social login (e.g., Google, GitHub), this can be a dummy value or the token received from the social provider", example = "admin")
    @NotBlank(message = InputValidateKey.PASSWORD_CAN_NOT_BE_NULL_OR_EMPTY)
    String password;

    @Schema(description = "Source of authentication, e.g., 'LOCAL', 'GOOGLE', 'GITHUB'", example = "local")
    @NotBlank(message = InputValidateKey.USER_SOURCE_CAN_NOT_BE_NULL_OR_EMPTY)
    @EnumValue(enumClass = UserSource.class, message = InputValidateKey.INVALID_USER_SOURCE_VALUE)
    String source;
}
