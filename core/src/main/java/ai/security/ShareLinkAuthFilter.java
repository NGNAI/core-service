package ai.security;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.web.filter.OncePerRequestFilter;

import ai.entity.postgres.ShareLinkEntity;
import ai.enums.ApiResponseStatus;
import ai.service.ShareLinkService;
import ai.util.ServletUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

/**
 * Filter xác thực public share link cho path {@code /public/share/**}.
 * <p>
 * Cơ chế:
 * <ul>
 *   <li>Trích token từ path variable (URL token — ai có link mới vào được).</li>
 *   <li>Lấy password từ header {@code X-Share-Password} (không qua URL để tránh lộ trong log).</li>
 *   <li>Gọi {@link ShareLinkService#validateAccess} kiểm tra revoked/expired/password.</li>
 *   <li>Nếu OK: put {@link ShareLinkEntity} vào request attribute {@code shareLink} cho controller + tăng view count async.</li>
 *   <li>Nếu lỗi: trả JSON response với error code tương ứng.</li>
 * </ul>
 * Filter này không set SecurityContext (endpoint public, không có JWT).
 */
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ShareLinkAuthFilter extends OncePerRequestFilter {

    /** Request attribute key chứa ShareLinkEntity đã validate. */
    public static final String SHARE_LINK_ATTR = "shareLink";
    /** Header name cho password (nếu link yêu cầu password). */
    public static final String PASSWORD_HEADER = "X-Share-Password";

    /** Pattern trích token từ /public/share/{token} và các sub-path. */
    static final Pattern SHARE_TOKEN_PATTERN = Pattern.compile("^/public/share/([^/]+).*$");

    ShareLinkService shareLinkService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        return !servletPath.startsWith("/public/share/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String servletPath = request.getServletPath();
        Matcher matcher = SHARE_TOKEN_PATTERN.matcher(servletPath);
        if (!matcher.matches()) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = matcher.group(1);
        String password = request.getHeader(PASSWORD_HEADER);

        try {
            ShareLinkEntity link = shareLinkService.validateAccess(token, password);
            // Tăng view count async (không block response)
            shareLinkService.incrementViewCount(link.getId());
            // Put vào request attribute cho controller
            request.setAttribute(SHARE_LINK_ATTR, link);
            filterChain.doFilter(request, response);
        } catch (ai.exception.AppException ex) {
            log.debug("Share link access denied for token {}: {}", token, ex.getApiResponseStatus().getMessage());
            ServletUtil.makeResponse(response, ex.getApiResponseStatus());
        } catch (Exception ex) {
            log.warn("Unexpected error validating share link token {}: {}", token, ex.getMessage());
            ServletUtil.makeResponse(response, ApiResponseStatus.UNEXPECTED);
        }
    }
}