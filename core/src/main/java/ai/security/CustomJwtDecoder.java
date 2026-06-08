package ai.security;

import java.util.Objects;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import ai.AppProperties;
import ai.dto.own.request.IntrospectRequestDto;
import ai.service.AuthService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Component
public class CustomJwtDecoder implements JwtDecoder {
    AuthService authService;
    AppProperties appProperties;

    @NonFinal
    NimbusJwtDecoder nimbusJwtDecoder;

    @Override
    public Jwt decode(String token) throws JwtException {
        boolean isValid = authService.introspect(IntrospectRequestDto.builder().token(token).build()).isValid();
        //System.out.println("############################ Token: " + token + ", isValid: " + isValid);

        if(!isValid) {
            //System.out.println("############################ Token invalid: " + token);
            throw new BadJwtException("Token invalid");
        }

        if(Objects.isNull(nimbusJwtDecoder)){
            SecretKeySpec secretKeySpec = new SecretKeySpec(appProperties.getJwt().getSecretKey().getBytes(), "HS512");

            nimbusJwtDecoder = NimbusJwtDecoder.withSecretKey(secretKeySpec).macAlgorithm(MacAlgorithm.HS512).build();
        }

        return nimbusJwtDecoder.decode(token);
    }
}
