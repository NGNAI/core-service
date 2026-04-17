package ai.dto.own.request;

import java.util.Set;

import ai.annotation.EnumValue;
import ai.constant.InputValidateKey;
import ai.enums.RagScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoteBookCreateConversationRequestDto {
    @NotBlank(message = InputValidateKey.MESSAGE_CAN_NOT_BE_NULL_OR_EMPTY)
    String message;

    @NotEmpty(message = InputValidateKey.RAG_SCOPE_CAN_NOT_BE_NULL_OR_EMPTY)
    Set<
            @EnumValue(enumClass = RagScope.class, message = InputValidateKey.INVALID_RAG_SCOPE_VALUE)
            String> scopes;
}
