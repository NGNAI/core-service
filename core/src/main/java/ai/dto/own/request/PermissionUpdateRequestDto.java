package ai.dto.own.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PermissionUpdateRequestDto {
    @NotBlank(message = "PERMISSION_NAME_CAN_NOT_BE_NULL_OR_EMPTY")
    String name;
    String description;
}
