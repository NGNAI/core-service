package ai.dto.own.request;

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
    @NotBlank(message = "USERNAME_CAN_NOT_BE_NULL_OR_EMPTY")
    String username;
    @NotBlank(message = "PASSWORD_CAN_NOT_BE_NULL_OR_EMPTY")
    String password;
    @NotBlank(message = "SOURCE_CAN_NOT_BE_NULL_OR_EMPTY")
    @Schema(description = "Source of authentication, e.g., 'LOCAL', 'GOOGLE', 'GITHUB'", example = "local")
    String source;
}
