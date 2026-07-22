package ai.util;

import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

@UtilityClass
public class JwtUtil {
    public Jwt getJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return null;
        }

        return jwt;
    }

    public UUID getUserId() {
        Jwt jwt = getJwt();
        return jwt != null ? UUID.fromString(jwt.getClaimAsString("user_id")) : null;
    }

    public UUID getOrgId() {
        Jwt jwt = getJwt();
        return jwt != null ? UUID.fromString(jwt.getClaimAsString("org_id")) : null;
    }

    /**
     * Lấy username của user hiện tại từ JWT subject claim.
     * JWT subject (sub) được set bằng {@code UserEntity.getUserName()} khi tạo token.
     *
     * @return username, hoặc {@code null} nếu không có JWT hợp lệ.
     */
    public String getUserName() {
        Jwt jwt = getJwt();
        return jwt != null ? jwt.getSubject() : null;
    }
}
