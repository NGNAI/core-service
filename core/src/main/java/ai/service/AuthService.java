package ai.service;

import ai.AppProperties;
import ai.dto.own.request.AuthRequestDto;
import ai.dto.own.request.IntrospectRequestDto;
import ai.dto.own.response.AuthResponseDto;
import ai.dto.own.response.IntrospectResponseDto;
import ai.entity.postgres.UserEntity;
import ai.enums.ApiResponseStatus;
import ai.exeption.AppException;
import ai.mapper.UserMapper;
import ai.repository.UserRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class AuthService {
    UserRepository userRepository;

    PasswordEncoder passwordEncoder;

    UserMapper userMapper;

    AppProperties appProperties;

    public IntrospectResponseDto introspect(IntrospectRequestDto introspectRequestDto){
        boolean isValid = false;
        try {
            isValid = verifyToken(introspectRequestDto.getToken());
        } catch (JOSEException | ParseException ex) {
            log.info("Token {} is invalid", introspectRequestDto.getToken(), ex);
        }
        return IntrospectResponseDto.builder().valid(isValid).build();
    }

    public AuthResponseDto auth(AuthRequestDto authRequestDto) throws JOSEException {
        UserEntity userEntity = userRepository.findByUserName(authRequestDto.getUsername())
                .orElseThrow(() -> new AppException(ApiResponseStatus.AUTHENTICATE_FAILED));

        if(!passwordEncoder.matches(authRequestDto.getPassword(),userEntity.getPassword())){
            throw new AppException(ApiResponseStatus.AUTHENTICATE_FAILED);
        }

        String token = generateToken(userEntity);

        return AuthResponseDto.builder()
                .token(token)
                .user(userMapper.entityToResponseDto(userEntity))
                .build();
    }

    private boolean verifyToken(String token) throws JOSEException, ParseException {
        JWSVerifier jwsVerifier = new MACVerifier(appProperties.getJwt().getSecretKey());
        SignedJWT signedJWT = SignedJWT.parse(token);
        return signedJWT.verify(jwsVerifier) && !signedJWT.getJWTClaimsSet().getExpirationTime().before(new Date());
    }

    private String generateToken(UserEntity userEntity) throws JOSEException {
        JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS512);
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(userEntity.getUserName())
                .issuer("NGN")
                .issueTime(new Date())
                .expirationTime(new Date(Instant.now().plus(appProperties.getJwt().getExpiryDuration(), ChronoUnit.SECONDS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope",buildJwtScope(userEntity))
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(jwsHeader,payload);

        jwsObject.sign(new MACSigner(appProperties.getJwt().getSecretKey()));

        return jwsObject.serialize();
    }

    private String buildJwtScope(UserEntity userEntity){
        /*FIXME build user scope*/
        StringJoiner stringJoiner = new StringJoiner(" ");

//        if(!CollectionUtils.isEmpty(userEntity.getRoles())){
//            userEntity.getRoles().forEach(role ->{
//                stringJoiner.add(role.getName());
//
//                if(!CollectionUtils.isEmpty(role.getPermissions())){
//                    role.getPermissions().forEach(permission ->{
//                        stringJoiner.add(permission.getName());
//                    });
//                }
//            });
//        }

        return stringJoiner.toString();
    }
}
