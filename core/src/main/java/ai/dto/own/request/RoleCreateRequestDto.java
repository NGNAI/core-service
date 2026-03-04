package ai.dto.own.request;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleCreateRequestDto {
    String name;
    String description;
    boolean defaultAssign;
}
