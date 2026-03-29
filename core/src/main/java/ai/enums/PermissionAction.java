package ai.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum PermissionAction {
    ALL("ALL", "All actions"),
    READ("READ", "Read"),
    CREATE("CREATE", "Create"),
    UPDATE("UPDATE", "Update"),
    DELETE("DELETE", "Delete"),
    ASSIGN("ASSIGN", "Assign"),
    REMOVE("REMOVE", "Remove");

    String key;
    String name;
}