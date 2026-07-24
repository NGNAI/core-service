package ai.service;

import ai.AppProperties;
import ai.dto.outer.otp.request.OtpAuthRequestDto;
import ai.dto.outer.otp.response.OtpAuthResponseDto;
import ai.dto.own.request.audit.AuditLogRequest;
import ai.dto.own.request.AuthRequestDto;
import ai.dto.own.request.IntrospectRequestDto;
import ai.dto.own.request.OrganizationSelectRequestDto;
import ai.dto.own.response.*;
import ai.entity.postgres.OrganizationEntity;
import ai.entity.postgres.OrganizationUserRoleEntity;
import ai.entity.postgres.RoleEntity;
import ai.entity.postgres.UserEntity;
import ai.enums.ApiResponseStatus;
import ai.enums.AuditAction;
import ai.enums.AuditResource;
import ai.enums.TokenType;
import ai.exception.AppException;
import ai.mapper.OrganizationMapper;
import ai.mapper.RoleMapper;
import ai.mapper.UserMapper;
import ai.model.OtpApiResponseModel;
import ai.repository.OrganizationRepository;
import ai.repository.OrganizationUserRoleRepository;
import ai.repository.RoleRepository;
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
    AuditLogService auditLogService;
    SystemSettingService systemSettingService;

    UserRepository userRepository;
    OrganizationUserRoleRepository ourRepository;
    OrganizationRepository organizationRepository;
    RoleRepository roleRepository;

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
        }
        return IntrospectResponseDto.builder().valid(isValid).build();
    }

    public AuthResponseDto auth(AuthRequestDto authRequestDto) throws JOSEException, JsonProcessingException {
        try {
            UserEntity userEntity;
            if (authRequestDto.getSource().equals("local")) {
                userEntity = userRepository.findByUserNameAndSource(authRequestDto.getUsername(), "local")
                        .orElseThrow(() -> new AppException(ApiResponseStatus.AUTHENTICATE_FAILED));

                // Kiểm tra tài khoản có bị khoá do đăng nhập sai quá nhiều lần không
                if (userEntity.getLockedUntil() != null && userEntity.getLockedUntil().isAfter(Instant.now())) {
                    log.warn("Tài khoản {} đang bị khoá đến {}", authRequestDto.getUsername(), userEntity.getLockedUntil());
                    throw new AppException(ApiResponseStatus.USER_ACCOUNT_LOCKED);
                }

                if (!passwordEncoder.matches(authRequestDto.getPassword(), userEntity.getPassword())) {
                    // Đăng nhập sai: tăng bộ đếm loginAttempts, khoá tài khoản nếu vượt giới hạn
                    int maxAttempts = systemSettingService.getInt("security.maxLoginAttempts", 5);
                    int attempts = userEntity.getLoginAttempts() + 1;
                    userEntity.setLoginAttempts(attempts);
                    if (attempts >= maxAttempts) {
                        // Khoá tài khoản theo cấu hình security.accountLockDuration (phút), fallback 30 phút
                        int lockMinutes = systemSettingService.getInt("security.accountLockDuration", 30);
                        userEntity.setLockedUntil(Instant.now().plus(lockMinutes, ChronoUnit.MINUTES));
                    }
                    userRepository.save(userEntity);
                    throw new AppException(ApiResponseStatus.AUTHENTICATE_FAILED);
                }
            } else {
                OtpAuthRequestDto otpAuthRequestDto = new OtpAuthRequestDto(authRequestDto.getUsername(), authRequestDto.getPassword(), "ngn");
                OtpApiResponseModel<OtpAuthResponseDto> authResponse = otpApiService.auth(otpAuthRequestDto);
                if (authResponse.isSuccess()) {
                    OtpAuthResponseDto authResponseDto = authResponse.getData();

                    Optional<UserEntity> existingLdapUser = userRepository.findByUserNameAndSource(authResponseDto.getUserId(), "ldap");
                    boolean isNewUser = existingLdapUser.isEmpty();

                    // Kiểm tra conflict username: nếu user mới và đã có user local cùng username → báo lỗi
                    if (isNewUser && userRepository.existsByUserName(authResponseDto.getUserId())) {
                        throw new AppException(ApiResponseStatus.USER_EXISTED);
                    }

                    userEntity = existingLdapUser.orElseGet(() -> {
                        UserEntity newUser = new UserEntity();
                        newUser.setSource("ldap");
                        newUser.setPassword("");    
                        newUser.setActive(true);
                        newUser.setGender(0);
                        return newUser;
                    });

                    userEntity.setUserName(authResponseDto.getUserId());

                    // Chỉ cập nhật thông tin từ OTP nếu updateOnLogin = true HOẶC user mới tạo
                    boolean updateOnLogin = appProperties.getLdap() != null && appProperties.getLdap().isUpdateOnLogin();
                    if (isNewUser || updateOnLogin) {
                        userEntity.setEmail(authResponseDto.getEmail());
                        userEntity.setFirstName(authResponseDto.getFullName());
                    }
                    userEntity.setSource("ldap");
                    userEntity.setPassword("");
                } else {
                    throw new AppException(ApiResponseStatus.AUTHENTICATE_FAILED);
                }
            }

            // Đăng nhập thành công: reset bộ đếm loginAttempts và xoá lockedUntil
            userEntity.setLoginAttempts(0);
            userEntity.setLockedUntil(null);
            userEntity.setLastLogin(Instant.now());
            userEntity = userRepository.save(userEntity);

            // Auto-assign org mặc định cho user LDAP nếu chưa có org nào
            if (authRequestDto.getSource().equals("ldap")) {
                boolean hasNoOrg = ourRepository.findByUserWithPermission(userEntity.getId()).isEmpty();
                if (hasNoOrg) {
                    assignDefaultOrg(userEntity);
                }
            }

            UserWithOrgResponseDto userResponse = userMapper.entityToWithOrgResponseDto(userEntity);

            Map<UUID, OrganizationWithUserRoleDto> mapResult = new HashMap<>();
            Map<UUID, Map<String, Map<String, Map<String, String>>>> mapRolePermission = roleService.getPermissionListOfRole();

            ourRepository.findByUserWithPermission(userResponse.getId()).forEach(our -> {
                UUID orgId = our.getOrganization().getId();
                RoleEntity roleEntity = our.getRole();
                RoleSimplifyResponseDto role = roleMapper.entityToSimplifyResponseDto(roleEntity);
                role.setPermissions(mapRolePermission.getOrDefault(role.getId(), Map.of()));

                if (!mapResult.containsKey(orgId)) {
                    OrganizationWithUserRoleDto orgWithRole = organizationMapper.entityToWithUserRoleResponseDto(our.getOrganization());
                    orgWithRole.getRoles().add(role);
                    mapResult.put(orgId, orgWithRole);
                } else
                    mapResult.get(orgId).getRoles().add(role);
            });

            userResponse.getOrganizations().addAll(mapResult.values());

            if (userResponse.getOrganizations().isEmpty())
                throw new AppException(ApiResponseStatus.USER_NOT_IN_ORG);

            String token = userResponse.getOrganizations().size() == 1
                    ? generateToken(userEntity, TokenType.ACCESS, userResponse.getOrganizations().iterator().next().getId())
                    : generateToken(userEntity, TokenType.TEMP, null);

            auditLogService.record(AuditLogRequest.builder()
                    .action(AuditAction.LOGIN)
                    .resource(AuditResource.AUTH)
                    .userId(userEntity.getId())
                    .userName(userEntity.getUserName())
                    .success(true)
                    .description("Đăng nhập thành công: " + userEntity.getUserName())
                    .build());

            return AuthResponseDto.builder()
                    .token(token)
                    .user(userResponse)
                    .build();
        } catch (Exception ex) {
            auditLogService.record(AuditLogRequest.builder()
                    .action(AuditAction.LOGIN)
                    .resource(AuditResource.AUTH)
                    .userName(authRequestDto.getUsername())
                    .success(false)
                    .errorMessage(ex.getMessage())
                    .description("Đăng nhập thất bại: " + authRequestDto.getUsername())
                    .build());
            throw ex instanceof AppException appEx
                    ? appEx
                    : new AppException(ApiResponseStatus.AUTHENTICATE_FAILED);
        }
    }

    public OrganizationSelectResponseDto selectOrg(OrganizationSelectRequestDto requestDto) throws JOSEException {
        UUID orgId = requestDto.getOrgId();
        UUID userId = JwtUtil.getUserId();
        UserEntity userEntity = userService.getEntityById(userId);
        organizationService.validateOrgId(orgId);
        List<OrganizationUserRoleEntity> ours = ourRepository.findByUserAndOrgWithPermission(userId, orgId);

        if(ours.isEmpty())
            throw new AppException(ApiResponseStatus.USER_NOT_EXIST_IN_ORGANIZATION);

        OrganizationWithUserRoleDto organizationWithUserRoleDto = new OrganizationWithUserRoleDto();
        OrganizationEntity organizationEntity = ours.getFirst().getOrganization();
        organizationWithUserRoleDto.setName(organizationEntity.getName());
        organizationWithUserRoleDto.setDescription(organizationEntity.getDescription());

        Map<UUID, Map<String, Map<String, Map<String, String>>>> mapRolePermission = roleService.getPermissionListOfRole();
        ours.forEach(our->{
            RoleEntity roleEntity = our.getRole();
            RoleSimplifyResponseDto role = roleMapper.entityToSimplifyResponseDto(roleEntity);
            role.setPermissions(mapRolePermission.getOrDefault(role.getId(),Map.of()));

            organizationWithUserRoleDto.getRoles().add(role);
        });
        OrganizationSelectResponseDto response = OrganizationSelectResponseDto.builder()
                .token(generateToken(userEntity, TokenType.ACCESS, orgId))
                .organization(organizationWithUserRoleDto)
                .build();

        auditLogService.record(AuditLogRequest.builder()
                .action(AuditAction.SELECT_ORG)
                .resource(AuditResource.AUTH)
                .userId(userId)
                .userName(userEntity.getUserName())
                .orgId(orgId)
                .organizationName(organizationEntity.getName())
                .resourceId(orgId.toString())
                .resourceName(organizationEntity.getName())
                .success(true)
                .description("Chọn tổ chức: " + organizationEntity.getName())
                .build());

        return response;
    }

    private boolean verifyToken(String token) throws JOSEException, ParseException {
        JWSVerifier jwsVerifier = new MACVerifier(appProperties.getJwt().getSecretKey());
        SignedJWT signedJWT = SignedJWT.parse(token);
        return signedJWT.verify(jwsVerifier) && !signedJWT.getJWTClaimsSet().getExpirationTime().before(new Date());
    }

    private String generateToken(UserEntity userEntity, TokenType type, UUID orgId) throws JOSEException {
        JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS512);
        // Đọc session timeout từ system settings (đơn vị: phút), fallback về appProperties nếu không có
        long sessionTimeoutMinutes = systemSettingService.getLong("security.sessionTimeout", 0);
        long expiryDurationSeconds = sessionTimeoutMinutes > 0
                ? sessionTimeoutMinutes * 60
                : appProperties.getJwt().getExpiryDuration();
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(userEntity.getUserName())
                .issuer("NGN")
                .issueTime(new Date())
                .expirationTime(new Date(Instant.now().plus(expiryDurationSeconds, ChronoUnit.SECONDS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .claim("type",type)
                .claim("scope",buildJwtScope(userEntity, orgId))
                .claim("user_id",userEntity.getId())
                .claim("org_id",Objects.requireNonNullElse(orgId,""))
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(jwsHeader,payload);

        jwsObject.sign(new MACSigner(appProperties.getJwt().getSecretKey()));

        return jwsObject.serialize();
    }

    private String buildJwtScope(UserEntity userEntity, UUID orgId){
        /*FIXME build user scope*/
        StringJoiner stringJoiner = new StringJoiner(" ");

//        if(!CollectionUtils.isEmpty(userEntity.g())){
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

    /**
     * Gán user LDAP vào organization mặc định + role mặc định (từ config LDAP).
     * Nếu không có config → skip (admin tự phân bổ sau).
     * Nếu user đã có org rồi → không gán lại.
     */
    private void assignDefaultOrg(UserEntity userEntity) {
        AppProperties.Ldap ldapConfig = appProperties.getLdap();
        if (ldapConfig == null) return;

        String defaultOrgId = ldapConfig.getDefaultOrgId();
        if (defaultOrgId == null || defaultOrgId.isBlank()) {
            log.info("Không có ldap.default-org-id config → skip auto-assign cho user {}", userEntity.getUserName());
            return;
        }

        UUID orgId;
        try {
            orgId = UUID.fromString(defaultOrgId);
        } catch (IllegalArgumentException e) {
            log.warn("ldap.default-org-id '{}' không phải UUID hợp lệ → skip", defaultOrgId);
            return;
        }

        OrganizationEntity orgEntity = organizationRepository.findById(orgId).orElse(null);
        if (orgEntity == null) {
            log.warn("ldap.default-org-id '{}' không tồn tại trong DB → skip auto-assign", defaultOrgId);
            return;
        }

        // Tìm role mặc định: dùng defaultRoleId từ config, hoặc fallback findByDefaultAssign()
        RoleEntity roleEntity = null;
        String defaultRoleId = ldapConfig.getDefaultRoleId();
        if (defaultRoleId != null && !defaultRoleId.isBlank()) {
            try {
                UUID roleId = UUID.fromString(defaultRoleId);
                roleEntity = roleRepository.findById(roleId).orElse(null);
            } catch (IllegalArgumentException e) {
                log.warn("ldap.default-role-id '{}' không phải UUID hợp lệ", defaultRoleId);
            }
        }
        if (roleEntity == null) {
            roleEntity = roleRepository.findByDefaultAssign().orElse(null);
        }
        if (roleEntity == null) {
            log.warn("Không tìm thấy role mặc định (defaultRoleId trống + không có role defaultAssign) → skip auto-assign cho user {}", userEntity.getUserName());
            return;
        }

        OrganizationUserRoleEntity our = new OrganizationUserRoleEntity(orgEntity, userEntity, roleEntity);
        ourRepository.save(our);
        log.info("Đã gán user LDAP '{}' vào org '{}' với role '{}'", userEntity.getUserName(), orgEntity.getName(), roleEntity.getName());
    }
}
