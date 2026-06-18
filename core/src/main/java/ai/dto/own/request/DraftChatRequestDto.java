package ai.dto.own.request;

import ai.constant.InputValidateKey;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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

    @Schema(description = "The content of the draft on editor, which can be used for generating content based on the change request. It should be provided when the user wants to generate content based on the change request.")
    String currentDraftContent;
}
