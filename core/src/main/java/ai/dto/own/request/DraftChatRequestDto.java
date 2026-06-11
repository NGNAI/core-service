package ai.dto.own.request;

import ai.constant.InputValidateKey;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DraftChatRequestDto {
    @NotBlank(message = InputValidateKey.MESSAGE_CAN_NOT_BE_NULL_OR_EMPTY)
    String message;

    @NotBlank(message = InputValidateKey.DRAFT_CONTENT_CAN_NOT_BE_NULL_OR_EMPTY)
    @Schema(description = "The content of the draft on editor, which can be used for generating content based on the change request. It should be provided when the user wants to generate content based on the change request.")
    String generatedContent;
}
