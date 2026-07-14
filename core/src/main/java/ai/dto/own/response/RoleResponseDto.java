package ai.dto.own.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

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
    UUID id;
    String name;
    String description;
    boolean defaultAssign;

    List<PermissionWithRoleScopeResponseDto> permissions;
}
