package ai.annotation.validator;

import ai.annotation.EnumValue;
import ai.annotation.StringValue;
import ai.enums.PermissionAction;
import ai.enums.PermissionResource;
import ai.enums.PermissionScope;
import ai.enums.RagScope;
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
        if(annotation.enumClass()==RagScope.class){
            acceptedValues.addAll(Stream.of(RagScope.values()).map(RagScope::getKey).toList());
        } else if(annotation.enumClass()== PermissionResource.class){
            acceptedValues.addAll(Stream.of(PermissionResource.values()).map(PermissionResource::getKey).toList());
        } else if(annotation.enumClass()== PermissionScope.class){
            acceptedValues.addAll(Stream.of(PermissionScope.values()).map(PermissionScope::getKey).toList());
        } else if(annotation.enumClass()== PermissionAction.class){
            acceptedValues.addAll(Stream.of(PermissionAction.values()).map(PermissionAction::getKey).toList());
        }
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Để @NotNull xử lý nếu cần
        }

        return acceptedValues.contains(value);
    }
}
