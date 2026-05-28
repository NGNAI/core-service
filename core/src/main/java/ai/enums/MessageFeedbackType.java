package ai.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum MessageFeedbackType {
    LIKE("like"),
    DISLIKE("dislike");

    String value;

    public static boolean isSupportedValue(String value) {
        return java.util.Arrays.stream(values())
                .anyMatch(type -> type.getValue().equalsIgnoreCase(value));
    }
}
