package ai.aspect;

import ai.annotation.Audited;
import ai.dto.own.request.audit.AuditLogRequest;
import ai.service.AuditLogService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuditAspect {

    AuditLogService auditLogService;

    @Around("@annotation(ai.annotation.Audited) && execution(* *(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Audited audited = method.getAnnotation(Audited.class);
        if (audited == null) {
            return joinPoint.proceed();
        }

        Object result;
        try {
            result = joinPoint.proceed();
            recordSuccess(joinPoint, audited, method);
            return result;
        } catch (Throwable ex) {
            recordFailure(joinPoint, audited, method, ex);
            throw ex;
        }
    }

    private void recordSuccess(ProceedingJoinPoint joinPoint, Audited audited, Method method) {
        try {
            Object[] args = joinPoint.getArgs();
            String resourceId = resolveResourceId(audited.resourceIdExpression(), args);
            String description = formatDescription(audited.description(), args);
            Map<String, Object> details = buildDetails(method, args);

            auditLogService.record(AuditLogRequest.builder()
                    .action(audited.action())
                    .resource(audited.resource())
                    .resourceId(resourceId)
                    .description(description)
                    .details(details)
                    .success(true)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to record success audit for {}: {}", method.getName(), e.getMessage());
        }
    }

    private void recordFailure(ProceedingJoinPoint joinPoint, Audited audited, Method method, Throwable ex) {
        try {
            Object[] args = joinPoint.getArgs();
            String resourceId = resolveResourceId(audited.resourceIdExpression(), args);
            String description = formatDescription(audited.description(), args);
            Map<String, Object> details = buildDetails(method, args);

            auditLogService.record(AuditLogRequest.builder()
                    .action(audited.action())
                    .resource(audited.resource())
                    .resourceId(resourceId)
                    .description(description)
                    .details(details)
                    .success(false)
                    .errorMessage(ex.getMessage())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to record failure audit for {}: {}", method.getName(), e.getMessage());
        }
    }

    private String resolveResourceId(String expression, Object[] args) {
        if (expression == null || expression.isBlank()) {
            return args != null && args.length > 0 && args[0] != null ? args[0].toString() : null;
        }
        // Light SpEL fallback: support literal "#arg[N]" or plain literal
        if (expression.startsWith("#arg")) {
            try {
                int idx = Integer.parseInt(expression.substring(4).replaceAll("[^0-9]", ""));
                return idx >= 0 && idx < args.length && args[idx] != null ? args[idx].toString() : null;
            } catch (NumberFormatException ignore) {
                return expression;
            }
        }
        return expression;
    }

    private String formatDescription(String template, Object[] args) {
        if (template == null || template.isBlank()) return null;
        try {
            return java.text.MessageFormat.format(template, args);
        } catch (Exception e) {
            return template;
        }
    }

    private Map<String, Object> buildDetails(Method method, Object[] args) {
        Map<String, Object> details = new HashMap<>();
        details.put("method", method.getDeclaringClass().getSimpleName() + "." + method.getName());
        if (args != null) {
            details.put("argsCount", args.length);
        }
        return details;
    }
}
