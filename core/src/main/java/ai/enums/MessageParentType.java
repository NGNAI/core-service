package ai.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.Arrays;
import java.util.Objects;

@AllArgsConstructor
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum MessageParentType {
    TOPIC("topic"),
    NOTEBOOK("notebook")

    ;
    String value;

    public static MessageParentType getByValue(String value) {
        return Arrays.stream(values())
                .filter(e -> Objects.equals(e.value, value))
                .findFirst()
                .orElse(null);
    }
}
