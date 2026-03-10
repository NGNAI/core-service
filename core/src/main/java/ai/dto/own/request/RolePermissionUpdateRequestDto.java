package ai.dto.own.request;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RolePermissionUpdateRequestDto {
    @NotNull(message = "ROLE_PERMISSION_CAN_NOT_BE_NULL")
    Set<Integer> permissionIds;
}
