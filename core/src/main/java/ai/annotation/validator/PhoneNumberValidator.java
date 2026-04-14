package ai.annotation.validator;

import ai.annotation.PhoneNumber;
import ai.annotation.StringValue;
import ai.util.ValidateUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.Arrays;

public class PhoneNumberValidator implements ConstraintValidator<PhoneNumber, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true; // Để @NotNull xử lý nếu cần
        }
        return ValidateUtil.isValidVietnamesePhone(value);
    }
}
