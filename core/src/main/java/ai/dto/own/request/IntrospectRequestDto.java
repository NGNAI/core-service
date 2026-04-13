package ai.dto.own.request;

import ai.constant.InputValidateKey;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IntrospectRequestDto {
    @NotBlank(message = InputValidateKey.TOKEN_CAN_NOT_BE_NULL_OR_EMPTY)
    String token;
}
