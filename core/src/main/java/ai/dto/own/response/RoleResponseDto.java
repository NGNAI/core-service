package ai.dto.own.response;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleResponseDto {
    String name;
    String description;

    Set<PermissionResponseDto> permissions;
}
