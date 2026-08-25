package ai.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * Trạng thái của góp ý/phản hồi
 */
@AllArgsConstructor
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum FeedbackStatus {
    PENDING("PENDING", "Chờ xử lý"),
    PROCESSING("PROCESSING", "Đang xử lý"),
    RESOLVED("RESOLVED", "Đã giải quyết"),
    REJECTED("REJECTED", "Đã từ chối");

    String key;
    String name;

    public static boolean isSupportedValue(String value) {
        return java.util.Arrays.stream(values())
                .anyMatch(status -> status.getKey().equalsIgnoreCase(value));
    }
}
