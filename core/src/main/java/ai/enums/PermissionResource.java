package ai.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum PermissionResource {
    ALL("ALL", "Tất cả"),
    ORG("ORG", "Đơn vị"),
    USER("USER", "Người dùng"),
    ROLE("ROLE", "Vai trò"),
    PERMISSION("PERMISSION", "Quyền");

    String key;
    String name;
}
