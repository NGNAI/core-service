package ai.dto.own.response;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleResponseDto {
    int id;
    String name;
    String description;

    Set<PermissionResponseDto> permissions = new HashSet<>();
}
