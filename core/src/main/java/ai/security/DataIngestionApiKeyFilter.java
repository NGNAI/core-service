package ai.security;

import java.io.IOException;
import java.security.MessageDigest;
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
public class DataIngestionApiKeyFilter extends OncePerRequestFilter {
    static Pattern DATA_INGESTION_DETAILS_PATTERN = Pattern.compile("^/user/data-ingestion/[^/]+$");
    static Pattern DATA_INGESTION_DOWNLOAD_PATTERN = Pattern.compile("^/user/data-ingestion/[^/]+/download$");
    static Pattern DATA_INGESTION_DOWNLOAD_URL_PATTERN = Pattern.compile("^/user/data-ingestion/[^/]+/download-url$");
    static Pattern DATA_INGESTION_WEBHOOK_PATTERN = Pattern.compile("^/user/data-ingestion/ingestion/webhook/status$");

    AppProperties appProperties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        boolean isGetProtectedPath = "GET".equalsIgnoreCase(request.getMethod())
            && (DATA_INGESTION_DETAILS_PATTERN.matcher(servletPath).matches()
            || DATA_INGESTION_DOWNLOAD_PATTERN.matcher(servletPath).matches()
            || DATA_INGESTION_DOWNLOAD_URL_PATTERN.matcher(servletPath).matches());

        boolean isWebhookPath = "POST".equalsIgnoreCase(request.getMethod())
            && DATA_INGESTION_WEBHOOK_PATTERN.matcher(servletPath).matches();

        return !isGetProtectedPath && !isWebhookPath;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String servletPath = request.getServletPath();
        boolean isWebhookPath = "POST".equalsIgnoreCase(request.getMethod())
                && DATA_INGESTION_WEBHOOK_PATTERN.matcher(servletPath).matches();

        if (hasBearerToken(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isWebhookPath && authenticateBySignatureIfValid(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String configuredApiKey = resolveConfiguredApiKey();
        if (configuredApiKey == null || configuredApiKey.isBlank()) {
            if (isWebhookPath) {
                ServletUtil.makeResponse(response, ApiResponseStatus.UNAUTHENTICATED);
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        String providedApiKey = request.getHeader(resolveHeaderName());
        if (providedApiKey == null || providedApiKey.isBlank()) {
            if (isWebhookPath) {
                ServletUtil.makeResponse(response, ApiResponseStatus.UNAUTHENTICATED);
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        if (!configuredApiKey.equals(providedApiKey)) {
            ServletUtil.makeResponse(response, ApiResponseStatus.UNAUTHENTICATED);
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                ApiKeyAuthenticationConstants.DATA_INGESTION_API_KEY_PRINCIPAL,
                null,
                AuthorityUtils.createAuthorityList(ApiKeyAuthenticationConstants.DATA_INGESTION_API_KEY_AUTHORITY));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private boolean authenticateBySignatureIfValid(HttpServletRequest request) {
        String expectedSignature = resolveWebhookSignature();
        if (expectedSignature == null || expectedSignature.isBlank()) {
            return false;
        }

        String providedSignature = resolveProvidedWebhookSignature(request);
        if (providedSignature == null || providedSignature.isBlank()) {
            return false;
        }

        if (!MessageDigest.isEqual(expectedSignature.getBytes(), providedSignature.getBytes())) {
            return false;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                ApiKeyAuthenticationConstants.DATA_INGESTION_WEBHOOK_SIGNATURE_PRINCIPAL,
                null,
                AuthorityUtils.createAuthorityList(ApiKeyAuthenticationConstants.DATA_INGESTION_WEBHOOK_SIGNATURE_AUTHORITY));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return true;
    }

    private boolean hasBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        return authorization != null && authorization.startsWith("Bearer ");
    }

    private String resolveConfiguredApiKey() {
        if (appProperties.getIntegration() == null || appProperties.getIntegration().getDataIngestionApi() == null) {
            return null;
        }

        return appProperties.getIntegration().getDataIngestionApi().getKey();
    }

    private String resolveHeaderName() {
        if (appProperties.getIntegration() == null || appProperties.getIntegration().getDataIngestionApi() == null) {
            return "X-API-KEY";
        }

        String configuredHeader = appProperties.getIntegration().getDataIngestionApi().getHeaderName();
        return configuredHeader == null || configuredHeader.isBlank() ? "X-API-KEY" : configuredHeader;
    }

    private String resolveWebhookSignature() {
        if (appProperties.getIntegration() == null || appProperties.getIntegration().getDataIngestionCallback() == null) {
            return null;
        }

        return appProperties.getIntegration().getDataIngestionCallback().getSignature();
    }

    private String resolveProvidedWebhookSignature(HttpServletRequest request) {
        String headerName = resolveWebhookSignatureHeaderName();
        String fromHeader = request.getHeader(headerName);
        if (fromHeader != null && !fromHeader.isBlank()) {
            return fromHeader.trim();
        }

        String paramName = resolveWebhookSignatureParamName();
        String fromParam = request.getParameter(paramName);
        if (fromParam != null && !fromParam.isBlank()) {
            return fromParam.trim();
        }

        return null;
    }

    private String resolveWebhookSignatureParamName() {
        if (appProperties.getIntegration() == null
                || appProperties.getIntegration().getDataIngestionCallback() == null
                || appProperties.getIntegration().getDataIngestionCallback().getSignatureParamName() == null
                || appProperties.getIntegration().getDataIngestionCallback().getSignatureParamName().isBlank()) {
            return "signature";
        }

        return appProperties.getIntegration().getDataIngestionCallback().getSignatureParamName();
    }

    private String resolveWebhookSignatureHeaderName() {
        if (appProperties.getIntegration() == null
                || appProperties.getIntegration().getDataIngestionCallback() == null
                || appProperties.getIntegration().getDataIngestionCallback().getSignatureHeaderName() == null
                || appProperties.getIntegration().getDataIngestionCallback().getSignatureHeaderName().isBlank()) {
            return "X-Webhook-Signature";
        }

        return appProperties.getIntegration().getDataIngestionCallback().getSignatureHeaderName();
    }
}
