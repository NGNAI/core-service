package ai.dto.own.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IntrospectRequestDto {
    @NotBlank(message = "TOKEN_CAN_NOT_BE_NULL_OR_EMPTY")
    String token;
}
