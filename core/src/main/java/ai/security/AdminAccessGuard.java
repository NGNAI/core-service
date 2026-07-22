package ai.security;

import java.util.List;

import ai.AppProperties;
import ai.util.JwtUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Guard bean kiểm tra quyền truy cập admin cho các resource nhạy cảm
 * (System Setting, System Health) dựa trên danh sách username hardcode
 * trong {@code application.yml} ({@code security.admin-allowed-usernames}),
 * thay vì qua hệ thống RBAC (Role/Permission).
 *
 * <p>Username được lấy từ JWT subject (sub) của request hiện tại.
 * Nếu danh sách cấu hình rỗng/null thì fallback về {@code ["root"]}.
 *
 * <p>Dùng trong {@code @PreAuthorize("@adminAccessGuard.isAllowed()")}.
 */
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service("adminAccessGuard")
public class AdminAccessGuard {

    /**
     * Fallback mặc định khi {@code security.admin-allowed-usernames} rỗng/null.
     */
    static final List<String> DEFAULT_ALLOWED_USERNAMES = List.of("root");

    AppProperties appProperties;

    /**
     * Kiểm tra xem user hiện tại (lấy từ JWT subject) có nằm trong danh sách
     * username được phép truy cập admin resource nhạy cảm hay không.
     *
     * @return {@code true} nếu username hiện tại nằm trong danh sách cho phép,
     *         {@code false} nếu không có JWT hợp lệ hoặc không nằm trong danh sách.
     */
    public boolean isAllowed() {
        String username = JwtUtil.getUserName();
        if (username == null || username.isBlank()) {
            log.debug("AdminAccessGuard: không có username trong JWT → từ chối");
            return false;
        }

        List<String> allowed = appProperties.getSecurity() != null
                ? appProperties.getSecurity().getAdminAllowedUsernames()
                : null;
        if (allowed == null || allowed.isEmpty()) {
            allowed = DEFAULT_ALLOWED_USERNAMES;
        }

        boolean permitted = allowed.contains(username);
        log.debug("AdminAccessGuard: username={} allowed={} → {}", username, allowed, permitted);
        return permitted;
    }
}