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
    PERMISSION("PERMISSION", "Quyền"),
    DATASET_PERSONAL("DATASET_PERSONAL", "Bộ dữ liệu cá nhân"),
    DATASET_LOCAL("DATASET_LOCAL", "Bộ dữ liệu đơn vị"),
    DATASET_GLOBAL("DATASET_GLOBAL", "Bộ dữ liệu công cộng"),
    DASHBOARD_GLOBAL("DASHBOARD_GLOBAL", "Bảng điều khiển toàn cầu");

    String key;
    String name;
}
