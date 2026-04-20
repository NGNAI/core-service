package ai.dto.own.request;

import ai.constant.InputValidateKey;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoteBookCreateRequestDto {
    @NotBlank(message = InputValidateKey.NOTEBOOK_TITLE_CAN_NOT_BE_NULL_OR_EMPTY)
    String title;

    String instruction;
}
