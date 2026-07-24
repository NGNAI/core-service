package ai.enums;

import java.util.Arrays;
import java.util.Objects;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * Loại tài nguyên được chia sẻ qua public share link.
 * Phù hợp với pattern {@link MessageParentType} của codebase.
 */
@AllArgsConstructor
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ShareResource {
    TOPIC("topic", "Chủ đề"),
    NOTEBOOK("notebook", "Sổ tay");

    String value;
    String name;

    public static ShareResource getByValue(String value) {
        return Arrays.stream(values())
                .filter(e -> Objects.equals(e.value, value))
                .findFirst()
                .orElse(null);
    }
}