package ai.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum PermissionAction {
    ALL("ALL", "Tất cả"),
    READ("READ", "Xem"),
    CREATE("CREATE", "Tạo"),
    UPDATE("UPDATE", "Sửa"),
    DELETE("DELETE", "Xoá"),
    ASSIGN("ASSIGN", "Phân bổ"),
    REMOVE("REMOVE", "Gỡ");

    String key;
    String name;
}