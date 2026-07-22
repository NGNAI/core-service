package ai.service;

import ai.AppProperties;
import ai.dto.outer.otp.response.OtpUserResponseDto;
import ai.dto.own.request.audit.AuditLogRequest;
import ai.entity.postgres.OrganizationEntity;
import ai.entity.postgres.OrganizationUserRoleEntity;
import ai.entity.postgres.RoleEntity;
import ai.entity.postgres.UserEntity;
import ai.enums.AuditAction;
import ai.enums.AuditResource;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Scheduler đồng bộ user LDAP từ OTP Service về DB local.
 * Có thể bật/tắt qua config ldap.sync.enabled (mặc định false).
 * Pattern theo DataIngestionMaintenanceScheduler: AtomicBoolean chống chạy chồng + try/catch/finally.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LdapSyncService {
    AppProperties appProperties;
    OtpApiService otpApiService;
    UserRepository userRepository;
    OrganizationRepository organizationRepository;
    RoleRepository roleRepository;
    OrganizationUserRoleRepository ourRepository;
    AuditLogService auditLogService;

    AtomicBoolean syncRunning = new AtomicBoolean(false);

    /**
     * Auto-sync user LDAP định kỳ.
     * Cron từ config ldap.sync.cron (mặc định 2h sáng mỗi ngày).
     * Chỉ chạy nếu ldap.sync.enabled = true.
     */
    @Scheduled(cron = "${ldap.sync.cron:0 0 2 * * ?}")
    public void syncLdapUsers() {
        AppProperties.Ldap ldapConfig = appProperties.getLdap();
        if (ldapConfig == null || ldapConfig.getSync() == null || !ldapConfig.getSync().isEnabled()) {
            return;
        }

        if (!syncRunning.compareAndSet(false, true)) {
            log.info("LDAP sync đang chạy, skip lần này");
            return;
        }

        int createdCount = 0;
        int updatedCount = 0;
        int skippedCount = 0;

        try {
            log.info("Bắt đầu đồng bộ user LDAP từ OTP Service...");

            // Lấy org + role mặc định
            OrganizationEntity defaultOrg = getDefaultOrg(ldapConfig);
            RoleEntity defaultRole = getDefaultRole(ldapConfig);

            // Phân trang lấy all user từ OTP
            int page = 0;
            int size = 100;
            boolean hasMore = true;

            while (hasMore) {
                OtpApiResponseModel<List<OtpUserResponseDto>> response = otpApiService.getAllUsers(page, size);
                if (response == null || !response.isSuccess() || response.getData() == null) {
                    log.warn("OTP Service trả về response không hợp lệ tại page {}", page);
                    break;
                }

                List<OtpUserResponseDto> users = response.getData();
                if (users.isEmpty()) {
                    break;
                }

                for (OtpUserResponseDto otpUser : users) {
                    try {
                        Optional<UserEntity> existingUser = userRepository.findByUserNameAndSource(otpUser.getUserId(), "ldap");

                        if (existingUser.isPresent()) {
                            // Đã có → cập nhật thông tin nếu updateOnLogin = true
                            if (ldapConfig.isUpdateOnLogin()) {
                                UserEntity user = existingUser.get();
                                user.setEmail(otpUser.getEmail());
                                user.setFirstName(otpUser.getFullName());
                                user.setPhoneNumber(otpUser.getPhoneNumber());
                                userRepository.save(user);
                                updatedCount++;
                            } else {
                                skippedCount++;
                            }
                        } else {
                            // Chưa có → kiểm tra conflict username
                            if (userRepository.existsByUserName(otpUser.getUserId())) {
                                log.warn("Skip user '{}': username đã tồn tại (conflict với user local)", otpUser.getUserId());
                                skippedCount++;
                                continue;
                            }

                            // Tạo mới
                            UserEntity newUser = new UserEntity();
                            newUser.setUserName(otpUser.getUserId());
                            newUser.setEmail(otpUser.getEmail() != null ? otpUser.getEmail() : otpUser.getUserId() + "@ldap.local");
                            newUser.setFirstName(otpUser.getFullName() != null ? otpUser.getFullName() : otpUser.getUserId());
                            newUser.setPhoneNumber(otpUser.getPhoneNumber());
                            newUser.setSource("ldap");
                            newUser.setPassword("");
                            newUser.setActive(true);
                            newUser.setGender(0);
                            newUser = userRepository.save(newUser);

                            // Gán vào org mặc định + role mặc định
                            if (defaultOrg != null && defaultRole != null) {
                                OrganizationUserRoleEntity our = new OrganizationUserRoleEntity(defaultOrg, newUser, defaultRole);
                                ourRepository.save(our);
                            }

                            createdCount++;
                        }
                    } catch (Exception e) {
                        log.error("Lỗi khi sync user '{}': {}", otpUser.getUserId(), e.getMessage(), e);
                        skippedCount++;
                    }
                }

                // Nếu số user trả về < size → hết
                hasMore = users.size() >= size;
                page++;
            }

            // Ghi audit log tổng kết
            auditLogService.record(AuditLogRequest.builder()
                    .action(AuditAction.ASSIGN)
                    .resource(AuditResource.ORG_USER_ROLE)
                    .success(true)
                    .description(String.format("Đồng bộ LDAP hoàn tất: %d user mới, %d user cập nhật, %d user skip",
                            createdCount, updatedCount, skippedCount))
                    .build());

            log.info("Đồng bộ LDAP hoàn tất: {} user mới, {} user cập nhật, {} user skip", createdCount, updatedCount, skippedCount);

        } catch (Exception e) {
            log.error("Lỗi khi đồng bộ LDAP", e);
        } finally {
            syncRunning.set(false);
        }
    }

    private OrganizationEntity getDefaultOrg(AppProperties.Ldap ldapConfig) {
        String defaultOrgId = ldapConfig.getDefaultOrgId();
        if (defaultOrgId == null || defaultOrgId.isBlank()) {
            log.warn("ldap.default-org-id để trống → user mới sẽ KHÔNG được gán org khi sync");
            return null;
        }
        try {
            UUID orgId = UUID.fromString(defaultOrgId);
            return organizationRepository.findById(orgId).orElse(null);
        } catch (IllegalArgumentException e) {
            log.warn("ldap.default-org-id '{}' không phải UUID hợp lệ", defaultOrgId);
            return null;
        }
    }

    private RoleEntity getDefaultRole(AppProperties.Ldap ldapConfig) {
        String defaultRoleId = ldapConfig.getDefaultRoleId();
        if (defaultRoleId != null && !defaultRoleId.isBlank()) {
            try {
                UUID roleId = UUID.fromString(defaultRoleId);
                return roleRepository.findById(roleId).orElse(null);
            } catch (IllegalArgumentException e) {
                log.warn("ldap.default-role-id '{}' không phải UUID hợp lệ", defaultRoleId);
            }
        }
        return roleRepository.findByDefaultAssign().orElse(null);
    }
}