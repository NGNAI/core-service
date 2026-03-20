package ai.dto.own.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConversationRequestDto {
    @NotBlank(message = "MESSAGE_CAN_NOT_BE_NULL_OR_EMPTY")
    String message;
}
