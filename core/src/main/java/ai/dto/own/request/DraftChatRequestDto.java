package ai.dto.own.request;

import java.util.Set;

import ai.annotation.EnumValue;
import ai.constant.InputValidateKey;
import ai.enums.RagScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class DraftChatRequestDto {
    @NotBlank(message = InputValidateKey.MESSAGE_CAN_NOT_BE_NULL_OR_EMPTY)
    String message;

    @NotEmpty(message = InputValidateKey.RAG_SCOPE_CAN_NOT_BE_NULL_OR_EMPTY)
    Set<
            @EnumValue(enumClass = RagScope.class, message = InputValidateKey.INVALID_RAG_SCOPE_VALUE)
            String> scopes;

    @Schema(description = "The content of the draft on editor, which can be used for generating content based on the change request. It should be provided when the user wants to generate content based on the change request.")
    String currentDraftContent;
}
