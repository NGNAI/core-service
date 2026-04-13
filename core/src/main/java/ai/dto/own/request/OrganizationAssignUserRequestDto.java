package ai.dto.own.request;

import ai.constant.InputValidateKey;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrganizationAssignUserRequestDto {
    @NotEmpty(message = InputValidateKey.USER_IDS_CAN_NOT_BE_NULL_OR_EMPTY)
    Set<UUID> userIds;
    UUID roleId;
}
