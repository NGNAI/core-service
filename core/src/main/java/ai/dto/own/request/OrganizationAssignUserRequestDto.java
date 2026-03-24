package ai.dto.own.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.Set;
import java.util.UUID;

@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrganizationAssignUserRequestDto {
    @NotEmpty(message = "USER_IDS_CAN_NOT_BE_NULL_OR_EMPTY")
    Set<UUID> userIds;
    UUID roleId;
}
