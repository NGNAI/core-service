package ai.dto.own.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.Map;
import java.util.UUID;

@JsonPropertyOrder({
        "id",
        "name",
        "permissions",
})
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleSimplifyResponseDto {
    UUID id;
    String name;
    Map<String, Map<String, Map<String, String>>> permissions;
}
