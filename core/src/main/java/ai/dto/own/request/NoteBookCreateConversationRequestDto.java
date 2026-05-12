package ai.dto.own.request;

import java.util.Set;

import ai.constant.InputValidateKey;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoteBookCreateConversationRequestDto {
    @NotBlank(message = InputValidateKey.MESSAGE_CAN_NOT_BE_NULL_OR_EMPTY)
    String message;

    Set<String> sourceIds;
}
