package ai.dto.own.request;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PermissionAssignRequestDto {
    @NotNull(message = "ASSIGN_PERMISSION_ID_CAN_NOT_BE_NULL")
    UUID id;
    @NotNull(message = "ASSIGN_PERMISSION_SCOPE_CAN_NOT_BE_NULL")
    String scope;
}
