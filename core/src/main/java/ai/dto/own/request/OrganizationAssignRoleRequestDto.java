package ai.dto.own.request;

import ai.constant.InputValidateKey;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.Set;
import java.util.UUID;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrganizationAssignRoleRequestDto {
    @NotEmpty(message = InputValidateKey.USER_IDS_CAN_NOT_BE_NULL_OR_EMPTY)
    Set<UUID> userIds;
}
