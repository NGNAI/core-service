package ai.logging;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Set;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Filter log toàn bộ HTTP request & response (method, URI, query params,
 * headers, body request, status code, body response, thời gian xử lý).
 * <p>
 * Chỉ active khi dùng Spring profile {@code dev} (xem {@link LoggingConfig}).
 * <p>
 * Thay thế cho các class như CustomURLFilter, LoggingService,
 * CustomRequestBodyAdviceAdapter, CustomResponseBodyAdviceAdapter.
 */
@Slf4j
public class RequestLoggingInterceptor implements Filter {

    private static final String MDC_KEY = "request_id";
    private static final Set<String> SENSITIVE_HEADERS = Set.of(
            "authorization", "cookie", "set-cookie", "x-api-key", "auth-token"
    );
    private static final Set<String> SKIP_HEADERS = Set.of(
            "content-length", "host", "connection"
    );

    @Override
    public void init(FilterConfig filterConfig) {
        // no-op
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest httpReq) || !(response instanceof HttpServletResponse httpRes)) {
            chain.doFilter(request, response);
            return;
        }

        String requestId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put(MDC_KEY, requestId);

        ContentCachingRequestWrapper wrappedReq = new ContentCachingRequestWrapper(httpReq, 1024 * 1024 * 12); // 12MB
        ContentCachingResponseWrapper wrappedRes = new ContentCachingResponseWrapper(httpRes);

        long start = System.currentTimeMillis();
        logRequest(wrappedReq, requestId);

        try {
            chain.doFilter(wrappedReq, wrappedRes);
        } finally {
            logResponse(wrappedReq, wrappedRes, requestId, System.currentTimeMillis() - start);
            wrappedRes.copyBodyToResponse();
            MDC.remove(MDC_KEY);
        }
    }

    @Override
    public void destroy() {
        // no-op
    }

    private void logRequest(ContentCachingRequestWrapper request, String requestId) {
        StringBuilder data = new StringBuilder();
        data.append("\n--> [").append(requestId).append("] ")
                .append(request.getMethod()).append(" ").append(request.getRequestURI());

        if (request.getQueryString() != null) {
            data.append("?").append(request.getQueryString());
        }
        data.append("\n");

        // Headers
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = headerNames.nextElement();
            String value;
            if (SENSITIVE_HEADERS.contains(key.toLowerCase())) {
                value = "***";
            } else if (SKIP_HEADERS.contains(key.toLowerCase())) {
                continue;
            } else {
                value = request.getHeader(key);
            }
            data.append("  ").append(key).append(": ").append(value).append("\n");
        }

        log.info(data.toString());
    }

    private void logResponse(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response,
                             String requestId, long durationMs) {
        StringBuilder data = new StringBuilder();
        data.append("\n<-- [").append(requestId).append("] ")
                .append(request.getMethod()).append(" ").append(request.getRequestURI())
                .append(" -> ").append(response.getStatus())
                .append(" (").append(durationMs).append("ms)\n");

        // Response body
        byte[] buf = response.getContentAsByteArray();
        if (buf.length > 0) {
            String body = new String(buf, java.nio.charset.StandardCharsets.UTF_8);
            if (body.length() < 2000) {
                data.append("  body: ").append(body).append("\n");
            } else {
                data.append("  body: (").append(body.length()).append(" bytes)\n");
            }
        }

        log.info(data.toString());
    }
}
