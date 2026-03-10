package ai.dto.own.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;

@JsonPropertyOrder({
        "id",
        "name",
        "description",
        "defaultAssign",
        "permissions"
})
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleResponseDto extends AuditResponseDto {
    int id;
    String name;
    String description;
    boolean defaultAssign;

    Set<PermissionResponseDto> permissions;
}
