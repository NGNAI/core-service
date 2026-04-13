package ai.annotation.validator;

import ai.annotation.EnumValue;
import ai.annotation.StringValue;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class EnumValuesValidator implements ConstraintValidator<EnumValue, String> {
    List<String> acceptedValues = new ArrayList<>();

    @Override
    public void initialize(EnumValue annotation) {
        acceptedValues = Stream.of(annotation.enumClass().getEnumConstants())
                .map(Enum::name)
                .toList();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Để @NotNull xử lý nếu cần
        }

        return acceptedValues.contains(value);
    }
}
