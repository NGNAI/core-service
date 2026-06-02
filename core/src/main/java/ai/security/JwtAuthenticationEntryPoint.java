package ai.security;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import ai.enums.ApiResponseStatus;
import ai.util.ServletUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        System.out.println("############################ Unauthorized error: " + authException.getMessage());
        ServletUtil.makeResponse(response, ApiResponseStatus.UNAVAILABLE_FOR_LEGAL_REASONS);
    }
}
