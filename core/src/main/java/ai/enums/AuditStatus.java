package ai.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum AuditStatus {
    SUCCESS("SUCCESS", "Thành công"),
    FAILED("FAILED", "Thất bại");

    String key;
    String name;
}
