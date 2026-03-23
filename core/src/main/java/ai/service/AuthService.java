package ai.service;

import ai.AppProperties;
import ai.dto.outer.otp.request.OtpAuthRequestDto;
import ai.dto.outer.otp.response.OtpAuthResponseDto;
import ai.dto.own.request.AuthRequestDto;
import ai.dto.own.request.IntrospectRequestDto;
import ai.dto.own.request.OrganizationSelectRequestDto;
import ai.dto.own.request.filter.RoleFilterDto;
import ai.dto.own.response.*;
import ai.entity.postgres.OrganizationUserRoleEntity;
import ai.entity.postgres.RoleEntity;
import ai.entity.postgres.UserEntity;
import ai.enums.ApiResponseStatus;
import ai.enums.TokenType;
import ai.exeption.AppException;
import ai.mapper.OrganizationMapper;
import ai.mapper.RoleMapper;
import ai.mapper.UserMapper;
import ai.model.OtpApiResponseModel;
import ai.repository.OrganizationUserRoleRepository;
import ai.repository.UserRepository;
import ai.service.api.OtpApiService;
import ai.util.JwtUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
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

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class AuthService {
    OtpApiService otpApiService;
    UserService userService;
    OrganizationService organizationService;
    RoleService roleService;

    UserRepository userRepository;
    OrganizationUserRoleRepository ourRepository;

    PasswordEncoder passwordEncoder;

    UserMapper userMapper;
    OrganizationMapper organizationMapper;
    RoleMapper roleMapper;

    AppProperties appProperties;

    public IntrospectResponseDto introspect(IntrospectRequestDto introspectRequestDto){
        boolean isValid = false;
        try {
            isValid = verifyToken(introspectRequestDto.getToken());
        } catch (JOSEException | ParseException ex) {
            log.info("Token {} is invalid", introspectRequestDto.getToken(), ex);
            ex.printStackTrace();
        }
        return IntrospectResponseDto.builder().valid(isValid).build();
    }

    public AuthResponseDto auth(AuthRequestDto authRequestDto) throws JOSEException, JsonProcessingException {
        UserEntity userEntity = null;
        if(authRequestDto.getSource().equals("local")){
            userEntity = userRepository.findByUserNameAndSource(authRequestDto.getUsername(),"local")
                    .orElseThrow(() -> new AppException(ApiResponseStatus.AUTHENTICATE_FAILED));

            if(!passwordEncoder.matches(authRequestDto.getPassword(),userEntity.getPassword()))
                throw new AppException(ApiResponseStatus.AUTHENTICATE_FAILED);
        } else {
            OtpAuthRequestDto otpAuthRequestDto = new OtpAuthRequestDto(authRequestDto.getUsername(),authRequestDto.getPassword(),"ngn");
            OtpApiResponseModel<OtpAuthResponseDto> authResponse = otpApiService.auth(otpAuthRequestDto);
            if(authResponse.isSuccess()) {
                OtpAuthResponseDto authResponseDto = authResponse.getData();

                userEntity = userRepository.findByUserNameAndSource(authResponseDto.getUserId(),"ldap")
                        .orElse(new UserEntity());

                userEntity.setUserName(authResponseDto.getUserId());
                userEntity.setEmail(authResponseDto.getEmail());
                userEntity.setFirstName(authResponseDto.getFullName());
                userEntity.setSource("ldap");

                //FIXME remove this after recreate database
                userEntity.setPassword("");
            } else {
                throw new AppException(ApiResponseStatus.AUTHENTICATE_FAILED);
            }
        }
        userEntity.setLastLogin(Instant.now());
        userEntity = userRepository.save(userEntity);

        UserWithOrgResponseDto userResponse = userMapper.entityToWithOrgResponseDto(userEntity);

        Map<Integer, OrganizationWithUserRoleDto> mapResult = new HashMap<>();
        RoleFilterDto roleFilter = new RoleFilterDto();
        roleFilter.setPageSize(20);

        Map<Integer,Set<String>> mapRolePermission = roleService.getPermissionListOfRole(roleFilter);

        ourRepository.findByUserWithPermission(userResponse.getId()).forEach(our->{
            int orgId = our.getOrganization().getId();
            RoleEntity roleEntity = our.getRole();
            RoleSimplifyResponseDto role = roleMapper.entityToSimplifyResponseDto(roleEntity);
            role.setPermissions(mapRolePermission.getOrDefault(role.getId(),Set.of()));

            if(!mapResult.containsKey(orgId)) {
                OrganizationWithUserRoleDto orgWithRole = organizationMapper.entityToWithUserRoleResponseDto(our.getOrganization());

                orgWithRole.getRoles().add(role);

                mapResult.put(orgId, orgWithRole);
            } else
                mapResult.get(orgId).getRoles().add(role);
        });

        userResponse.getOrganizations().addAll(mapResult.values());

        if(userResponse.getOrganizations().isEmpty())
            throw new AppException(ApiResponseStatus.USER_NOT_IN_ORG);

        String token = userResponse.getOrganizations().size()==1
                ? generateToken(userEntity, TokenType.ACCESS, userResponse.getOrganizations().iterator().next().getId())
                : generateToken(userEntity, TokenType.TEMP, null);

        return AuthResponseDto.builder()
                .token(token)
                .user(userResponse)
                .build();
    }

    public OrganizationSelectResponseDto selectOrg(OrganizationSelectRequestDto requestDto) throws JOSEException {
        int orgId = requestDto.getOrgId();
        int userId = JwtUtil.getUserId();
        UserEntity userEntity = userService.getEntityById(userId);
        organizationService.validateOrgId(orgId);
        List<OrganizationUserRoleEntity> ours = ourRepository.findByUserAndOrgWithPermission(userId, orgId);

        if(ours.isEmpty())
            throw new AppException(ApiResponseStatus.USER_NOT_EXIST_IN_ORGANIZATION);

        OrganizationWithUserRoleDto organizationWithUserRoleDto = new OrganizationWithUserRoleDto();
        RoleFilterDto roleFilter = new RoleFilterDto();
        roleFilter.setPageSize(20);

        Map<Integer,Set<String>> mapRolePermission = roleService.getPermissionListOfRole(roleFilter);
        ours.forEach(our->{
            RoleEntity roleEntity = our.getRole();
            RoleSimplifyResponseDto role = roleMapper.entityToSimplifyResponseDto(roleEntity);
            role.setPermissions(mapRolePermission.getOrDefault(role.getId(),Set.of()));

            organizationWithUserRoleDto.getRoles().add(role);
        });
        return OrganizationSelectResponseDto.builder()
                .token(generateToken(userEntity, TokenType.ACCESS, orgId))
                .organization(organizationWithUserRoleDto)
                .build();
    }

    private boolean verifyToken(String token) throws JOSEException, ParseException {
        JWSVerifier jwsVerifier = new MACVerifier(appProperties.getJwt().getSecretKey());
        SignedJWT signedJWT = SignedJWT.parse(token);
        return signedJWT.verify(jwsVerifier) && !signedJWT.getJWTClaimsSet().getExpirationTime().before(new Date());
    }

    private String generateToken(UserEntity userEntity, TokenType type, Integer orgId) throws JOSEException {
        JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS512);
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(userEntity.getUserName())
                .issuer("NGN")
                .issueTime(new Date())
                .expirationTime(new Date(Instant.now().plus(appProperties.getJwt().getExpiryDuration(), ChronoUnit.SECONDS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .claim("type",type)
                .claim("scope",buildJwtScope(userEntity))
                .claim("user_id",userEntity.getId())
                .claim("org_id",Objects.requireNonNullElse(orgId,-1))
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
