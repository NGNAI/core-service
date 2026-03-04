package ai.dto.own.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrganizationResponseDto {
    int id;
    String name;
    String description;
    Integer parentId;
    Set<OrganizationResponseDto> children;
}
