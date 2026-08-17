package ai.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import java.util.Optional;
import java.util.UUID;

@EnableScheduling
@EnableJpaAuditing(auditorAwareRef = "auditorAware") // Enable audit for entity
@Configuration
public class ApplicationConfig {
    @Bean
    PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder(10);
    }

    /**
     * User tường minh cho basic auth của Actuator (/actuator/prometheus, /actuator/env, ...).
     * Dùng InMemoryUserDetailsManager thay vì phụ thuộc auto-config `spring.security.user`
     * (vì project có bean PasswordEncoder tùy chỉnh — một số phiên bản Spring Security/Boot
     * sẽ back-off và không tự tạo user, khiến basic auth luôn trả 401).
     *
     * CHỈ phục vụ chain /actuator/** (httpBasic). KHÔNG dùng cho login người dùng (JWT).
     * Password lấy từ biến môi trường ACTUATOR_USER_PASSWORD, mặc định "changeme" (chỉ cho dev).
     */
    @Bean
    UserDetailsService actuatorUserDetailsService(
            PasswordEncoder passwordEncoder,
            @Value("${ACTUATOR_USER_PASSWORD:changeme}") String actuatorPassword) {
        return new InMemoryUserDetailsManager(
                User.withUsername("monitor")
                        .password(passwordEncoder.encode(actuatorPassword))
                        .roles("ACTUATOR")
                        .build());
    }

    @Bean
    ObjectMapper objectMapper(){
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    @Bean
    public AuditorAware<UUID> auditorAware() {
        return () -> {
            Authentication authentication =
                    SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.empty();
            }

            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();

                UUID userId = UUID.fromString(jwt.getClaimAsString("user_id"));
                return Optional.of(userId);
            }

            return Optional.empty();
        };
    }
}
