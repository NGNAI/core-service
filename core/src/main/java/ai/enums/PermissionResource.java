package ai.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum PermissionResource {
    ALL("ALL", "All resources"),
    ORG("ORG", "Organization"),
    USER("USER", "User"),
    ROLE("ROLE", "Role"),
    PERMISSION("PERMISSION", "Permission");

    String key;
    String name;
}
