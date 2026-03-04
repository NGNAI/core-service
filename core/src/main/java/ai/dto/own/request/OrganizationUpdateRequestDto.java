package ai.dto.own.request;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrganizationUpdateRequestDto {
    String name;
    String description;
    Integer parentId;
}
