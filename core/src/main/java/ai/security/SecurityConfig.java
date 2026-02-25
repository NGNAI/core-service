package ai.security;

import ai.AppProperties;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@EnableWebSecurity
@EnableMethodSecurity
@Configuration
public class SecurityConfig {
    CustomJwtDecoder customJWTDecoder;

    @Bean
    public SecurityFilterChain jwtFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .securityMatcher("/pub/**", "/swagger-ui/**", "/v3/api-docs*/**", "/prv/**")
                .authorizeHttpRequests(request ->
                    request.requestMatchers("/pub/**","/swagger-ui/**","/v3/api-docs*/**").permitAll()
                            .requestMatchers("/prv/**").authenticated()
                    )
                .oauth2ResourceServer(oauth2 ->
                    oauth2.jwt(jwt ->
                            jwt.decoder(customJWTDecoder)
                                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        ).authenticationEntryPoint(new JwtAuthenticationEntryPoint())
                    )
                .csrf(AbstractHttpConfigurer::disable);

        return httpSecurity.build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter(){
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);

        return jwtAuthenticationConverter;
    }

}
