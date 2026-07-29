package ai.service;

import ai.AppProperties;
import ai.dto.outer.otp.response.OtpUserResponseDto;
import ai.dto.own.request.audit.AuditLogRequest;
import ai.dto.own.request.ldap.LdapImportRequestDto;
import ai.dto.own.response.ldap.LdapImportResponseDto;
import ai.dto.own.response.ldap.LdapUserResponseDto;
import ai.entity.postgres.OrganizationEntity;
import ai.entity.postgres.OrganizationUserRoleEntity;
import ai.entity.postgres.RoleEntity;
import ai.entity.postgres.UserEntity;
import ai.enums.ApiResponseStatus;
import ai.enums.AuditAction;
import ai.enums.AuditResource;
import ai.exception.AppException;
import ai.model.OtpApiResponseModel;
import ai.repository.OrganizationRepository;
import ai.repository.OrganizationUserRoleRepository;
import ai.repository.RoleRepository;
import ai.repository.UserRepository;
import ai.service.api.OtpApiService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service xử lý import user LDAP từ OTP Service vào hệ thống.
 * Hỗ trợ: search user LDAP, import nhiều user 1 lần, get chi tiết user.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LdapService {
    OtpApiService otpApiService;
    UserRepository userRepository;
    OrganizationRepository organizationRepository;
    RoleRepository roleRepository;
    OrganizationUserRoleRepository ourRepository;
    AuditLogService auditLogService;
    AppProperties appProperties;

    /**
     * Tìm kiếm user LDAP qua OTP Service (không filter org/role).
     */
    public List<LdapUserResponseDto> searchLdapUsers(String keyword) {
        return searchLdapUsers(keyword, null, null);
    }

    /**
     * Tìm kiếm user LDAP qua OTP Service, có lọc theo org và role.
     *
     * @param keyword        từ khoá tìm kiếm
     * @param excludeUsersInOrganizationId optional — nếu truyền sẽ loại bỏ user đã có trong org đó
     * @param excludeUsersInRoleId         optional — nếu truyền cùng excludeUsersInOrganizationId sẽ loại bỏ user đã có role đó trong org
     */
    public List<LdapUserResponseDto> searchLdapUsers(String keyword, UUID excludeUsersInOrganizationId, UUID excludeUsersInRoleId) {
        OtpApiResponseModel<List<OtpUserResponseDto>> response = otpApiService.searchUsers(keyword);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            return List.of();
        }

        List<OtpUserResponseDto> otpUsers = response.getData();

        // Tìm user LDAP đã tồn tại trong DB
        List<String> otpUserIds = otpUsers.stream().map(OtpUserResponseDto::getUserId).toList();
        List<UserEntity> existingUsers = userRepository.findByUserNameInAndSource(otpUserIds, "ldap");
        Map<String, UUID> userNameToUserId = existingUsers.stream()
                .collect(Collectors.toMap(UserEntity::getUserName, UserEntity::getId));

        // Nếu có orgId → xác định set user cần loại bỏ
        Set<String> excludedUserNames = new HashSet<>();
        if (excludeUsersInOrganizationId != null && !userNameToUserId.isEmpty()) {
            List<UUID> existingUuids = new ArrayList<>(userNameToUserId.values());
            List<OrganizationUserRoleEntity> oursInOrg = ourRepository.findByOrganizationIdAndUserIdIn(excludeUsersInOrganizationId, existingUuids);

            if (excludeUsersInRoleId != null) {
                // Chỉ loại bỏ user đã có role đó trong org
                Set<UUID> userIdsWithRole = oursInOrg.stream()
                        .filter(our -> our.getId().getRoleId().equals(excludeUsersInRoleId))
                        .map(our -> our.getId().getUserId())
                        .collect(Collectors.toSet());
                userNameToUserId.forEach((userName, uuid) -> {
                    if (userIdsWithRole.contains(uuid)) {
                        excludedUserNames.add(userName);
                    }
                });
            } else {
                // Loại bỏ tất cả user đã có trong org
                Set<UUID> userIdsInOrg = oursInOrg.stream()
                        .map(our -> our.getId().getUserId())
                        .collect(Collectors.toSet());
                userNameToUserId.forEach((userName, uuid) -> {
                    if (userIdsInOrg.contains(uuid)) {
                        excludedUserNames.add(userName);
                    }
                });
            }
        }

        // Build response, filter out excluded users
        return otpUsers.stream()
                .filter(otpUser -> !excludedUserNames.contains(otpUser.getUserId()))
                .map(otpUser -> {
                    UUID existingId = userNameToUserId.get(otpUser.getUserId());
                    return LdapUserResponseDto.builder()
                            .userId(otpUser.getUserId())
                            .fullName(otpUser.getFullName())
                            .email(otpUser.getEmail())
                            .phoneNumber(otpUser.getPhone1())
                            .organization(otpUser.getOrganization())
                            .domain(otpUser.getDomain())
                            .enable(otpUser.getEnable())
                            .imported(existingId != null)
                            .existingUserId(existingId)
                            .build();
                }).toList();
    }

    /**
     * Lấy chi tiết 1 user LDAP kèm trạng thái import.
     */
    public LdapUserResponseDto getLdapUserDetail(String ldapUserId) {
        OtpApiResponseModel<OtpUserResponseDto> response = otpApiService.getUserDetail(ldapUserId);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new AppException(ApiResponseStatus.USER_NOT_EXISTS);
        }

        OtpUserResponseDto otpUser = response.getData();
        Optional<UserEntity> existingUser = userRepository.findByUserNameAndSource(otpUser.getUserId(), "ldap");

        return LdapUserResponseDto.builder()
                .userId(otpUser.getUserId())
                .fullName(otpUser.getFullName())
                .email(otpUser.getEmail())
                .phoneNumber(otpUser.getPhone1())
                .organization(otpUser.getOrganization())
                .domain(otpUser.getDomain())
                .enable(otpUser.getEnable())
                .imported(existingUser.isPresent())
                .existingUserId(existingUser.map(UserEntity::getId).orElse(null))
                .build();
    }

    /**
     * Import một loạt user LDAP vào hệ thống.
     * Nếu có organizationId → gán user vào org + role luôn.
     * Nếu không → chỉ tạo user trong hệ thống (admin tự phân bổ org sau).
     * Mỗi user được xử lý độc lập trong transaction riêng (REQUIRES_NEW).
     */
    public LdapImportResponseDto importLdapUsers(LdapImportRequestDto request) {
        // Validate org (optional)
        OrganizationEntity orgEntity = null;
        RoleEntity roleEntity = null;

        UUID orgId = request.getOrganizationId();
        if (orgId != null) {
            orgEntity = organizationRepository.findById(orgId)
                    .orElseThrow(() -> new AppException(ApiResponseStatus.ORGANIZATION_NOT_EXISTS));
            roleEntity = resolveRole(request.getRoleId());
        }

        List<LdapImportResponseDto.LdapImportItemResult> results = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;

        for (String ldapUserId : request.getLdapUserIds()) {
            try {
                LdapImportResponseDto.LdapImportItemResult result = importSingleUser(ldapUserId, orgEntity, roleEntity);
                results.add(result);
                if (result.isSuccess()) {
                    successCount++;
                } else {
                    failedCount++;
                }
            } catch (Exception e) {
                log.error("Lỗi khi import user LDAP '{}': {}", ldapUserId, e.getMessage(), e);
                results.add(LdapImportResponseDto.LdapImportItemResult.builder()
                        .ldapUserId(ldapUserId)
                        .success(false)
                        .message("Lỗi: " + e.getMessage())
                        .build());
                failedCount++;
            }
        }

        return LdapImportResponseDto.builder()
                .successCount(successCount)
                .failedCount(failedCount)
                .results(results)
                .build();
    }

    /**
     * Import 1 user LDAP trong transaction riêng.
     * Nếu orgEntity == null → chỉ tạo/save user, không gán org.
     * Nếu orgEntity != null → tạo user + gán org (+ role).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LdapImportResponseDto.LdapImportItemResult importSingleUser(
            String ldapUserId, OrganizationEntity orgEntity, RoleEntity roleEntity) {
        // 1. Lấy thông tin user từ OTP Service
        OtpApiResponseModel<OtpUserResponseDto> otpResponse = otpApiService.getUserDetail(ldapUserId);
        if (otpResponse == null || !otpResponse.isSuccess() || otpResponse.getData() == null) {
            return LdapImportResponseDto.LdapImportItemResult.builder()
                    .ldapUserId(ldapUserId)
                    .success(false)
                    .message("Không tìm thấy user trong LDAP")
                    .build();
        }
        OtpUserResponseDto otpUser = otpResponse.getData();

        // 2. Kiểm tra user đã tồn tại trong DB chưa
        UserEntity userEntity;
        Optional<UserEntity> existingUser = userRepository.findByUserNameAndSource(otpUser.getUserId(), "ldap");

        if (existingUser.isPresent()) {
            // Đã có trong DB → cập nhật thông tin
            userEntity = existingUser.get();
            userEntity.setEmail(otpUser.getEmail());
            userEntity.setFirstName(otpUser.getFullName());
            userEntity.setPhoneNumber(otpUser.getPhone1());
            userEntity = userRepository.save(userEntity);
        } else {
            // Chưa có → kiểm tra conflict username với user local
            if (userRepository.existsByUserName(otpUser.getUserId())) {
                return LdapImportResponseDto.LdapImportItemResult.builder()
                        .ldapUserId(ldapUserId)
                        .success(false)
                        .message("Username đã tồn tại trong hệ thống (conflict với user local)")
                        .build();
            }

            // Tạo mới
            userEntity = new UserEntity();
            userEntity.setUserName(otpUser.getUserId());
            userEntity.setEmail(otpUser.getEmail() != null ? otpUser.getEmail() : otpUser.getUserId() + "@ldap.local");
            userEntity.setFirstName(otpUser.getFullName() != null ? otpUser.getFullName() : otpUser.getUserId());
            userEntity.setPhoneNumber(otpUser.getPhone1());
            userEntity.setSource("ldap");
            userEntity.setPassword("");
            userEntity.setActive(true);
            userEntity.setGender(0);
            userEntity = userRepository.save(userEntity);
        }

        // 3. Gán vào org + role (nếu có orgEntity)
        if (orgEntity != null) {
            List<OrganizationUserRoleEntity> existingOurs = ourRepository.findByOrganizationIdAndUserIdIn(
                    orgEntity.getId(), List.of(userEntity.getId()));
            if (existingOurs.isEmpty()) {
                OrganizationUserRoleEntity our = new OrganizationUserRoleEntity(orgEntity, userEntity, roleEntity);
                ourRepository.save(our);
            }

            // 4a. Ghi audit log (khi có gán org)
            auditLogService.record(AuditLogRequest.builder()
                    .action(AuditAction.ASSIGN)
                    .resource(AuditResource.ORG_USER_ROLE)
                    .userId(userEntity.getId())
                    .userName(userEntity.getUserName())
                    .orgId(orgEntity.getId())
                    .organizationName(orgEntity.getName())
                    .resourceId(orgEntity.getId().toString())
                    .resourceName(orgEntity.getName())
                    .success(true)
                    .description("Import user LDAP '" + ldapUserId + "' vào org '" + orgEntity.getName() + "'")
                    .build());
        } else {
            // 4b. Ghi audit log (không gán org)
            auditLogService.record(AuditLogRequest.builder()
                    .action(AuditAction.CREATE)
                    .resource(AuditResource.USER)
                    .userId(userEntity.getId())
                    .userName(userEntity.getUserName())
                    .success(true)
                    .description("Import user LDAP '" + ldapUserId + "' vào hệ thống (chưa gán org)")
                    .build());
        }

        return LdapImportResponseDto.LdapImportItemResult.builder()
                .ldapUserId(ldapUserId)
                .success(true)
                .message(orgEntity != null
                        ? "Import thành công vào org '" + orgEntity.getName() + "'"
                        : "Import thành công (chưa gán org)")
                .importedUserId(userEntity.getId().toString())
                .build();
    }

    /**
     * Resolve role: dùng roleId từ request, hoặc fallback findByDefaultAssign().
     */
    private RoleEntity resolveRole(UUID roleId) {
        if (roleId != null) {
            return roleRepository.findById(roleId)
                    .orElseThrow(() -> new AppException(ApiResponseStatus.ROLE_ID_NOT_EXISTS));
        }
        return roleRepository.findByDefaultAssign()
                .orElseThrow(() -> new AppException(ApiResponseStatus.ROLE_DEFAULT_ASSIGN_NOT_EXISTS));
    }

    /**
     * Lấy role mặc định từ config LDAP (dùng cho auto-assign khi login).
     */
    public RoleEntity getDefaultRole() {
        AppProperties.Ldap ldapConfig = appProperties.getLdap();
        if (ldapConfig != null && ldapConfig.getDefaultRoleId() != null && !ldapConfig.getDefaultRoleId().isBlank()) {
            try {
                UUID roleId = UUID.fromString(ldapConfig.getDefaultRoleId());
                return roleRepository.findById(roleId).orElse(null);
            } catch (IllegalArgumentException e) {
                log.warn("ldap.default-role-id '{}' không phải UUID hợp lệ", ldapConfig.getDefaultRoleId());
            }
        }
        return roleRepository.findByDefaultAssign().orElse(null);
    }

    /**
     * Lấy org mặc định từ config LDAP (dùng cho auto-assign khi login).
     */
    public OrganizationEntity getDefaultOrg() {
        AppProperties.Ldap ldapConfig = appProperties.getLdap();
        if (ldapConfig == null || ldapConfig.getDefaultOrgId() == null || ldapConfig.getDefaultOrgId().isBlank()) {
            return null;
        }
        try {
            UUID orgId = UUID.fromString(ldapConfig.getDefaultOrgId());
            return organizationRepository.findById(orgId).orElse(null);
        } catch (IllegalArgumentException e) {
            log.warn("ldap.default-org-id '{}' không phải UUID hợp lệ", ldapConfig.getDefaultOrgId());
            return null;
        }
    }
}