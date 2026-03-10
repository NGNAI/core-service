package ai.dto.own.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@JsonPropertyOrder({
        "id",
        "name",
        "description",
})
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PermissionResponseDto extends AuditResponseDto {
    int id;
    String name;
    String description;
}
