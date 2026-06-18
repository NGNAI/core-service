package ai.dto.own.request;

import ai.constant.InputValidateKey;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class DraftRollbackRequestDto {
    @NotBlank(message = InputValidateKey.DRAFT_ROLLBACK_REASON_CAN_NOT_BE_NULL_OR_EMPTY)
    String reason;
}
