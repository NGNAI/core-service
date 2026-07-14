package ai.util;

import ai.entity.postgres.UserEntity;
import ai.exception.AppException;
import ai.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RequestContextUtil {

    UserRepository userRepository;

    public HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    public String clientIp() {
        HttpServletRequest request = currentRequest();
        if (request == null) return null;

        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    public String userAgent() {
        HttpServletRequest request = currentRequest();
        if (request == null) return null;
        String ua = request.getHeader("User-Agent");
        if (ua != null && ua.length() > 500) {
            ua = ua.substring(0, 500);
        }
        return ua;
    }

    public String method() {
        HttpServletRequest request = currentRequest();
        return request != null ? request.getMethod() : null;
    }

    public String path() {
        HttpServletRequest request = currentRequest();
        if (request == null) return null;
        String uri = request.getRequestURI();
        if (uri != null && uri.length() > 500) {
            uri = uri.substring(0, 500);
        }
        return uri;
    }

    public String userName(UUID userId) {
        if (userId == null) return null;
        try {
            return userRepository.findById(userId).map(UserEntity::getUserName).orElse(null);
        } catch (Exception ignore) {
            return null;
        }
    }
}
