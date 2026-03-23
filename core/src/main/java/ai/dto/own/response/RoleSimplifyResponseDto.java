package ai.dto.own.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@JsonPropertyOrder({
        "id",
        "name",
        "permissions",
})
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleSimplifyResponseDto {
    int id;
    String name;
    Set<String> permissions;
}
