package ai.dto.own.request;

import ai.constant.InputValidateKey;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoteBookSourceAddTextRequestDto {
    @NotBlank(message = InputValidateKey.NOTE_CONTENT_CAN_NOT_BE_NULL_OR_EMPTY)
    @Schema(description = "Raw text content for TEXT source")
    String textContent;

    @Schema(description = "Display name for TEXT source")
    String displayName;
}