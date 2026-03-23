package ai.util;

import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@UtilityClass
public class JwtUtil {
    public Jwt getJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return null;
        }

        return jwt;
    }

    public int getUserId() {
        Jwt jwt = getJwt();
        return jwt != null ? Integer.parseInt(jwt.getClaimAsString("user_id")) : -1;
    }

    public int getOrgId() {
        Jwt jwt = getJwt();
        return jwt != null ? Integer.parseInt(jwt.getClaimAsString("org_id")) : -1;
    }
}
