package ai.security;

import java.io.IOException;
import java.util.regex.Pattern;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import ai.AppProperties;
import ai.enums.ApiResponseStatus;
import ai.util.ServletUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AttachmentApiKeyFilter extends OncePerRequestFilter {
    static Pattern ATTACHMENT_DETAILS_PATTERN = Pattern.compile("^/user/attachment/[^/]+$");
    static Pattern ATTACHMENT_DOWNLOAD_PATTERN = Pattern.compile("^/user/attachment/[^/]+/download$");
    static Pattern ATTACHMENT_DOWNLOAD_URL_PATTERN = Pattern.compile("^/user/attachment/[^/]+/download-url$");

    AppProperties appProperties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String servletPath = request.getServletPath();
        return !ATTACHMENT_DETAILS_PATTERN.matcher(servletPath).matches()
                && !ATTACHMENT_DOWNLOAD_PATTERN.matcher(servletPath).matches()
                && !ATTACHMENT_DOWNLOAD_URL_PATTERN.matcher(servletPath).matches();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Nếu request đã có Bearer token (tức là đã được xác thực bởi JwtAuthenticationFilter), thì không cần kiểm tra API key nữa
        if (hasBearerToken(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Kiểm tra API key trong header
        String configuredApiKey = resolveConfiguredApiKey();
        // Nếu API key không được cấu hình, thì không cần kiểm tra nữa, cho phép request đi tiếp
        if (configuredApiKey == null || configuredApiKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Kiểm tra API key được cung cấp trong header
        String providedApiKey = request.getHeader(resolveHeaderName());
        // Nếu không có API key nào được cung cấp, thì cho phép request đi tiếp để các filter khác xử lý (có thể trả về lỗi xác thực nếu endpoint yêu cầu authentication)
        if (providedApiKey == null || providedApiKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        // So sánh API key được cung cấp với API key đã cấu hình
        if (!configuredApiKey.equals(providedApiKey)) {
            ServletUtil.makeResponse(response, ApiResponseStatus.UNAUTHENTICATED);
            return;
        }

        // Nếu API key hợp lệ, tạo authentication token với authority đặc biệt để phân biệt với các loại authentication khác và set vào SecurityContext để các phần còn lại của ứng dụng có thể nhận biết được đây là request được xác thực bằng API key
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                ApiKeyAuthenticationConstants.ATTACHMENT_API_KEY_PRINCIPAL,
                null,
                AuthorityUtils.createAuthorityList(ApiKeyAuthenticationConstants.ATTACHMENT_API_KEY_AUTHORITY));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        // Set authentication vào SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    /**
     * Hàm này dùng để kiểm tra xem request có header Authorization với Bearer token hay không. Nếu có, tức là request đã được xác thực bằng JWT và filter này sẽ không can thiệp vào quá trình xác thực nữa. Nếu không có, filter sẽ tiếp tục kiểm tra API key.
     * @param request
     * @return
     */
    private boolean hasBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        return authorization != null && authorization.startsWith("Bearer ");
    }

    /**
     * Hàm này dùng để lấy API key đã được cấu hình trong application properties. Nếu API key không được cấu hình, trả về null. Nếu có lỗi trong quá trình lấy cấu hình, cũng trả về null. Việc trả về null sẽ giúp cho filter biết rằng không có API key nào được cấu hình và do đó không cần kiểm tra API key cho các request.
     * @return
     */
    private String resolveConfiguredApiKey() {
        if (appProperties.getIntegration() == null || appProperties.getIntegration().getAttachmentApi() == null) {
            return null;
        }

        return appProperties.getIntegration().getAttachmentApi().getKey();
    }

    /**
     * Hàm này dùng để xác định tên header mà client sẽ sử dụng để gửi API key. Mặc định là "X-API-KEY", nhưng có thể được cấu hình lại trong application properties. Việc tách hàm này ra giúp cho việc cấu hình trở nên linh hoạt hơn mà không cần phải thay đổi code của filter.
     * @return
     */
    private String resolveHeaderName() {
        if (appProperties.getIntegration() == null || appProperties.getIntegration().getAttachmentApi() == null) {
            return "X-API-KEY";
        }

        String configuredHeader = appProperties.getIntegration().getAttachmentApi().getHeaderName();
        return configuredHeader == null || configuredHeader.isBlank() ? "X-API-KEY" : configuredHeader;
    }
}