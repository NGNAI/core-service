package ai.dto.own.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.Data;
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
        "action"
})
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PermissionResponseDto extends AuditResponseDto {
    UUID id;
    String name;
    String description;
    String code;
    String resource;
    String action;
}
