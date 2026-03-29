package ai.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum PermissionScope {
    ALL("ALL", "All scope"),
    OWN("OWN", "Own resource"),
    DESCENDANT("DESCENDANT", "Descendant resources");

    String key;
    String name;
}