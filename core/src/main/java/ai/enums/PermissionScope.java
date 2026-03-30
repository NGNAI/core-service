package ai.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum PermissionScope {
    ALL("ALL", "Tất cả"),
    OWN("OWN", "Cá nhân"),
    DESCENDANT("DESCENDANT", "Cấp dưới");

    String key;
    String name;
}