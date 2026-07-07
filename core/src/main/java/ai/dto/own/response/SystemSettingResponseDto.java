package ai.dto.own.response;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@JsonPropertyOrder({
        "id",
        "key",
        "value",
        "description",
        "type",
        "groupName",
        "isPublic",
        "isActive",
        "displayOrder"
})
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SystemSettingResponseDto {
    UUID id;
    String key;
    String value;
    String description;
    String type;
    String groupName;
    Boolean isPublic;
    Boolean isActive;
    Integer displayOrder;
}
