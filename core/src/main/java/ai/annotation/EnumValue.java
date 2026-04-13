package ai.annotation;

import ai.annotation.validator.EnumValuesValidator;
import ai.annotation.validator.StringValuesValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;
import java.util.List;

@Target({ElementType.FIELD,ElementType.METHOD,ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EnumValuesValidator.class)
@Documented
public @interface EnumValue {
    Class<? extends Enum<?>> enumClass();
    String message() default "Invalid value";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
