package ai.annotation.validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import ai.annotation.EnumValue;
import ai.enums.DataScope;
import ai.enums.DataSource;
import ai.enums.PermissionAction;
import ai.enums.PermissionResource;
import ai.enums.PermissionScope;
import ai.enums.RagScope;
import ai.enums.UserSource;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class EnumValuesValidator implements ConstraintValidator<EnumValue, CharSequence> {
    List<String> acceptedValues = new ArrayList<>();

    @Override
    public void initialize(EnumValue annotation) {
        if (annotation.enumClass() == UserSource.class) {
            acceptedValues.addAll(Stream.of(UserSource.values()).map(UserSource::getValue).toList());
        } if (annotation.enumClass() == RagScope.class) {
            acceptedValues.addAll(Stream.of(RagScope.values()).map(RagScope::getKey).toList());
        } else if (annotation.enumClass() == PermissionResource.class) {
            acceptedValues.addAll(Stream.of(PermissionResource.values()).map(PermissionResource::getKey).toList());
        } else if (annotation.enumClass() == PermissionScope.class) {
            acceptedValues.addAll(Stream.of(PermissionScope.values()).map(PermissionScope::getKey).toList());
        } else if (annotation.enumClass() == PermissionAction.class) {
            acceptedValues.addAll(Stream.of(PermissionAction.values()).map(PermissionAction::getKey).toList());
        } else if (annotation.enumClass() == DataScope.class) {
            acceptedValues.addAll(Stream.of(DataScope.values()).map(DataScope::name).toList());
        }else if (annotation.enumClass() == DataSource.class) {
            acceptedValues.addAll(Stream.of(DataSource.values()).map(DataSource::name).toList());
        } else {
            acceptedValues.addAll(Arrays.stream(annotation.enumClass().getEnumConstants()).map(e -> {
                try {
                    return (String) annotation.enumClass().getMethod("name").invoke(e);
                } catch (Exception ex) {
                    throw new RuntimeException("Enum class must have a name() method that returns a String");
                }
            }).toList());
        }


    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Để @NotNull xử lý nếu cần
        }

        return acceptedValues.contains(value.toString());
    }
}
