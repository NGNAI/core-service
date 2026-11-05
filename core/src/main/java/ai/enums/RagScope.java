package ai.enums;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

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

    public static String normalizeKey(String value) {
        if (value == null) {
            return null;
        }

        for (RagScope scope : values()) {
            if (scope.key.equalsIgnoreCase(value) || scope.name().equalsIgnoreCase(value)) {
                return scope.key;
            }
        }
        return value;
    }

    public static Set<String> normalizeKeys(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return values.stream()
                .map(RagScope::normalizeKey)
                .collect(Collectors.toSet());
    }
}
