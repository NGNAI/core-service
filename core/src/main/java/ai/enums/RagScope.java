package ai.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum RagScope {
    GLOBAL("global","Toàn hệ thống"),
    LOCAL("local","Đơn vị trực thuộc"),
    PERSONAL("personal","Cá nhân");

    String key;
    String name;
}
