package ai.annotation.validator;

import ai.annotation.StringValue;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.Arrays;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class StringValuesValidator implements ConstraintValidator<StringValue, String> {
    String[] allowedValues;
    boolean ignoreCase;

    @Override
    public void initialize(StringValue annotation) {
        this.allowedValues = annotation.acceptedValues();
        this.ignoreCase = annotation.ignoreCase();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Để @NotNull xử lý nếu cần
        }

        return Arrays.stream(allowedValues)
                .anyMatch(allowed -> ignoreCase
                        ? allowed.equalsIgnoreCase(value)
                        : allowed.equals(value));
    }
}
