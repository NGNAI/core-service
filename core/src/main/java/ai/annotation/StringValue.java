package ai.annotation;

import ai.annotation.validator.StringValuesValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StringValuesValidator.class)
@Documented
public @interface StringValue {
    String[] acceptedValues();
    boolean ignoreCase();

    String message() default "Invalid value";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
