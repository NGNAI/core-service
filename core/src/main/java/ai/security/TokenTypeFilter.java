package ai.security;

import ai.enums.ApiResponseStatus;
import ai.enums.TokenType;
import ai.util.ServletUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TokenTypeFilter extends OncePerRequestFilter {
    TokenType type;

    public TokenTypeFilter(TokenType type){
        this.type = type;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            TokenType tokenType = TokenType.valueOf(jwt.getClaim("type"));
            if(!tokenType.equals(this.type)){
                ServletUtil.makeResponse(response, ApiResponseStatus.UNAUTHENTICATED);
            }
        }

        filterChain.doFilter(request,response);
    }
}
