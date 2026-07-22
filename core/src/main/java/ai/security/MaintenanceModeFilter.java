package ai.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.enums.ApiResponseStatus;
import ai.model.ApiResponseModel;
import ai.service.SystemSettingService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

/**
 * Filter kiểm tra chế độ bảo trì hệ thống. Nếu setting {@code system.maintenanceMode} = true,
 * tất cả request không thuộc danh sách loại trừ (auth, actuator, public settings) sẽ bị chặn
 * với mã lỗi {@link ApiResponseStatus#SYSTEM_MAINTENANCE}.
 */
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MaintenanceModeFilter extends OncePerRequestFilter {

    SystemSettingService systemSettingService;
    ObjectMapper objectMapper;

    // Các path được phép truy cập kể cả khi đang bảo trì
    private static final String[] WHITELIST_PATHS = {
            "/auth",
            "/auth/introspect",
            "/auth/select-org",
            "/actuator",
            "/public/settings",
            "/swagger-ui",
            "/v3/api-docs"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        boolean maintenanceMode = systemSettingService.getBoolean("system.maintenanceMode", false);
        if (!maintenanceMode) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestUri = request.getRequestURI();
        // Loại trừ context path (ví dụ: /api/v1)
        String contextPath = request.getContextPath();
        String path = contextPath != null && requestUri.startsWith(contextPath)
                ? requestUri.substring(contextPath.length())
                : requestUri;

        // Kiểm tra xem request có thuộc whitelist không
        for (String whitelist : WHITELIST_PATHS) {
            if (path.startsWith(whitelist)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        // Chặn request và trả về lỗi bảo trì
        log.warn("Request bị chặn do hệ thống đang bảo trì: {}", requestUri);
        ApiResponseModel<Void> apiResponse = ApiResponseModel.<Void>builder()
                .status(ApiResponseStatus.SYSTEM_MAINTENANCE.getCode())
                .message(ApiResponseStatus.SYSTEM_MAINTENANCE.getMessage())
                .build();

        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}