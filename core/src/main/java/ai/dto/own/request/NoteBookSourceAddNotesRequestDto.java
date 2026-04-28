package ai.dto.own.request;

import ai.constant.InputValidateKey;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoteBookSourceAddNotesRequestDto {
    @NotEmpty(message = InputValidateKey.NOTE_IDS_CAN_NOT_BE_NULL_OR_EMPTY)
    @Schema(description = "Existing note ids for NOTE sources")
    List<UUID> noteIds;
}