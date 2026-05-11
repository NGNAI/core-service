package ai.security;

import ai.enums.ApiResponseStatus;
import ai.util.ServletUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        System.out.println("############################ Unauthorized error: " + authException.getMessage());
        ServletUtil.makeResponse(response, ApiResponseStatus.UNAUTHENTICATED);
    }
}
