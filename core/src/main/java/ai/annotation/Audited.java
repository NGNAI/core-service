package ai.annotation;

import ai.enums.AuditAction;
import ai.enums.AuditResource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation to indicate a service method should be audited.
 * The {@link ai.aspect.AuditAspect} will record an audit log entry
 * after a successful (non-exceptional) invocation and a FAILED entry
 * when the method throws.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Audited {
    AuditAction action();

    AuditResource resource();

    /**
     * Optional SpEL expression (or literal) used to extract a resource id from
     * method arguments. The expression is evaluated against an array of method
     * arguments. Defaults to the first argument when omitted.
     */
    String resourceIdExpression() default "";

    /**
     * Optional description template; can reference method arguments by index
     * (e.g. "Created user {0}").
     */
    String description() default "";
}
