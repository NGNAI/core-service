package ai.dto.own.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@JsonPropertyOrder({
        "id",
        "name",
        "description",
        "parentId",
        "children"
})
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrganizationResponseDto {
    UUID id;
    String name;
    String description;
    Integer parentId;
    Set<OrganizationResponseDto> children;
}
