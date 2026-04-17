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
public class DataIngestionApiKeyFilter extends OncePerRequestFilter {
    static Pattern DATA_INGESTION_DETAILS_PATTERN = Pattern.compile("^/user/data-ingestion/[^/]+$");
    static Pattern DATA_INGESTION_DOWNLOAD_PATTERN = Pattern.compile("^/user/data-ingestion/[^/]+/download$");
    static Pattern DATA_INGESTION_DOWNLOAD_URL_PATTERN = Pattern.compile("^/user/data-ingestion/[^/]+/download-url$");

    AppProperties appProperties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String servletPath = request.getServletPath();
        return !DATA_INGESTION_DETAILS_PATTERN.matcher(servletPath).matches()
                && !DATA_INGESTION_DOWNLOAD_PATTERN.matcher(servletPath).matches()
                && !DATA_INGESTION_DOWNLOAD_URL_PATTERN.matcher(servletPath).matches();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (hasBearerToken(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String configuredApiKey = resolveConfiguredApiKey();
        if (configuredApiKey == null || configuredApiKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedApiKey = request.getHeader(resolveHeaderName());
        if (providedApiKey == null || providedApiKey.isBlank()) {
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
}
