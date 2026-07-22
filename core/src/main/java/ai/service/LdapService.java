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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
     * Tìm kiếm user LDAP qua OTP Service.
     * Trả về danh sách user kèm trạng thái đã import trong hệ thống chưa.
     */
    public List<LdapUserResponseDto> searchLdapUsers(String keyword) {
        OtpApiResponseModel<List<OtpUserResponseDto>> response = otpApiService.searchUsers(keyword);
        if (response == null || !response.isSuccess() || response.getData() == null) {
            return List.of();
        }

        return response.getData().stream().map(otpUser -> {
            Optional<UserEntity> existingUser = userRepository.findByUserNameAndSource(otpUser.getUserId(), "ldap");
            return LdapUserResponseDto.builder()
                    .userId(otpUser.getUserId())
                    .fullName(otpUser.getFullName())
                    .email(otpUser.getEmail())
                    .phoneNumber(otpUser.getPhoneNumber())
                    .organization(otpUser.getOrganization())
                    .domain(otpUser.getDomain())
                    .enable(otpUser.getEnable())
                    .imported(existingUser.isPresent())
                    .existingUserId(existingUser.map(UserEntity::getId).orElse(null))
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
                .phoneNumber(otpUser.getPhoneNumber())
                .organization(otpUser.getOrganization())
                .domain(otpUser.getDomain())
                .enable(otpUser.getEnable())
                .imported(existingUser.isPresent())
                .existingUserId(existingUser.map(UserEntity::getId).orElse(null))
                .build();
    }

    /**
     * Import một loạt user LDAP vào hệ thống.
     * Mỗi user được xử lý độc lập trong transaction riêng (REQUIRES_NEW):
     * nếu 1 user fail thì tiếp tục user còn lại (partial success).
     * Mỗi user thành công đều được ghi audit log.
     */
    public LdapImportResponseDto importLdapUsers(LdapImportRequestDto request) {
        // Validate org
        UUID orgId = request.getOrganizationId();
        if (orgId == null) {
            throw new AppException(ApiResponseStatus.ORGANIZATION_NOT_EXISTS);
        }
        OrganizationEntity orgEntity = organizationRepository.findById(orgId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.ORGANIZATION_NOT_EXISTS));

        // Validate role: dùng request.roleId hoặc fallback findByDefaultAssign()
        RoleEntity roleEntity = resolveRole(request.getRoleId());

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
     * Nếu fail → throw exception → caller catch và ghi nhận fail.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public LdapImportResponseDto.LdapImportItemResult importSingleUser(String ldapUserId, OrganizationEntity orgEntity, RoleEntity roleEntity) {
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
            userEntity.setPhoneNumber(otpUser.getPhoneNumber());
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
            userEntity.setPhoneNumber(otpUser.getPhoneNumber());
            userEntity.setSource("ldap");
            userEntity.setPassword("");
            userEntity.setActive(true);
            userEntity.setGender(0);
            userEntity = userRepository.save(userEntity);
        }

        // 3. Gán vào org + role (nếu chưa có)
        List<OrganizationUserRoleEntity> existingOurs = ourRepository.findByOrganizationIdAndUserIdIn(orgEntity.getId(), List.of(userEntity.getId()));
        if (existingOurs.isEmpty()) {
            OrganizationUserRoleEntity our = new OrganizationUserRoleEntity(orgEntity, userEntity, roleEntity);
            ourRepository.save(our);
        }

        // 4. Ghi audit log
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

        return LdapImportResponseDto.LdapImportItemResult.builder()
                .ldapUserId(ldapUserId)
                .success(true)
                .message("Import thành công")
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