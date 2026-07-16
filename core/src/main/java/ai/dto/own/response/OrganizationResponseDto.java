package ai.dto.own.response;

import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@JsonPropertyOrder({
        "id",
        "name",
        "description",
        "parentId",
        "totalUser",
        "children"
})
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrganizationResponseDto {
    UUID id;
    String name;
    String description;
    UUID parentId;
    String path;
    Long totalUser;
    Set<OrganizationResponseDto> children;
}
