package ai.dto.own.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@JsonPropertyOrder({
        "id",
        "name",
        "description",
        "code",
        "resource",
        "action",
        "scope"
})
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PermissionWithRoleScopeResponseDto extends PermissionResponseDto {
    String scope;
}
