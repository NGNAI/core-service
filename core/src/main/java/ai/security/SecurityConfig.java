package ai.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.Cookie;
import org.springframework.util.StringUtils;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

import ai.AppProperties;
import ai.enums.TokenType;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@EnableWebSecurity
@EnableMethodSecurity
@Configuration
public class SecurityConfig {
    CustomJwtDecoder customJWTDecoder;
        AppProperties appProperties;

    @Bean
    @Order(1)
    public SecurityFilterChain actuatorFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .securityMatcher("/actuator/**")
                .authorizeHttpRequests(request -> request
                        // Cho phép truy cập public health/info để Spring Boot Admin Server có thể poll
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        // Các endpoint còn lại (env, beans, threaddump, heapdump...) cần xác thực basic
                        .anyRequest().authenticated()
                )
                .httpBasic(httpBasic -> {})
                .cors(cors -> {})
                .csrf(AbstractHttpConfigurer::disable);

        return httpSecurity.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain authFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .securityMatcher("/auth/**")
                .authorizeHttpRequests(request -> request
                        .requestMatchers(
                                "/auth",
                                "/auth/introspect",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers("/auth/select-org").authenticated()
                )
                .oauth2ResourceServer(oauth2 ->
                        oauth2
                        .bearerTokenResolver(request -> {
                                // 1. Ưu tiên 1: Thử lấy Bearer Token từ Header Authorization bằng cơ chế mặc định của Spring
                                DefaultBearerTokenResolver defaultResolver = new DefaultBearerTokenResolver();
                                String token = defaultResolver.resolve(request);
                                
                                // 2. Ưu tiên 2: Nếu Header không có, lùng sục trong HttpOnly Cookie
                                if (!StringUtils.hasText(token) && request.getCookies() != null) {
                                        for (Cookie cookie : request.getCookies()) {
                                                if ("AUTH_TOKEN".equals(cookie.getName())) { // Tên cookie do bạn quy định lúc login
                                                        token = cookie.getValue();
                                                        break;
                                                }
                                        }
                                }
                                System.out.println("############################ Token: " + token);
                                return token; // Trả về chuỗi JWT nhặt được cho Spring Security giải mã tiếp
                        })
                        .jwt(jwt ->
                                jwt.decoder(customJWTDecoder)
                                        .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                                .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
                                .accessDeniedHandler(new JwtAccessDeniedHandler())
                )
                //.addFilterAfter(new TokenTypeFilter(TokenType.TEMP), BearerTokenAuthenticationFilter.class)
                .cors(cors -> {})
                .csrf(AbstractHttpConfigurer::disable);

        return httpSecurity.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain accessFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .securityMatcher("/admin/**","/user/**","/category/**")
                .authorizeHttpRequests(request -> request
                        .requestMatchers("/admin/**", "/user/**", "/category/**").authenticated()
                )
                .oauth2ResourceServer(oauth2 ->
                        oauth2
                        .bearerTokenResolver(request -> {
                                // 1. Ưu tiên 1: Thử lấy Bearer Token từ Header Authorization bằng cơ chế mặc định của Spring
                                DefaultBearerTokenResolver defaultResolver = new DefaultBearerTokenResolver();
                                String token = defaultResolver.resolve(request);
                                
                                // 2. Ưu tiên 2: Nếu Header không có, lùng sục trong HttpOnly Cookie
                                if (!StringUtils.hasText(token) && request.getCookies() != null) {
                                        for (Cookie cookie : request.getCookies()) {
                                                if ("AUTH_TOKEN".equals(cookie.getName())) { // Tên cookie do bạn quy định lúc login
                                                        token = cookie.getValue();
                                                        break;
                                                }
                                        }
                                }
                                System.out.println("############################ Token: " + token);
                                return token; // Trả về chuỗi JWT nhặt được cho Spring Security giải mã tiếp
                        })
                        .jwt(jwt ->
                                jwt.decoder(customJWTDecoder)
                                        .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                                .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
                                .accessDeniedHandler(new JwtAccessDeniedHandler())
                )
                .addFilterBefore(new DataIngestionApiKeyFilter(appProperties), BearerTokenAuthenticationFilter.class)
                .addFilterBefore(new AttachmentApiKeyFilter(appProperties), BearerTokenAuthenticationFilter.class)
                .addFilterAfter(new TokenTypeFilter(TokenType.ACCESS), BearerTokenAuthenticationFilter.class)
                .cors(cors -> {})
                .csrf(AbstractHttpConfigurer::disable);

        return httpSecurity.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("*"); // Cho phép tất cả các nguồn gốc (origins) trong các yêu cầu CORS
        config.addAllowedMethod("*"); // Cho phép tất cả các phương thức HTTP trong các yêu cầu CORS
        config.addAllowedHeader("*"); // Cho phép tất cả các header trong các yêu cầu CORS
        config.setAllowCredentials(true); // Cho phép gửi cookie và thông tin xác thực trong các yêu cầu CORS

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter(){
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);

        return jwtAuthenticationConverter;
    }
}
