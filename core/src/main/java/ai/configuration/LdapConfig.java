package ai.configuration;

import ai.AppProperties;
import ai.entity.postgres.OrganizationEntity;
import ai.entity.postgres.RoleEntity;
import ai.repository.OrganizationRepository;
import ai.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Kiểm tra cấu hình LDAP khi application khởi động.
 * Nếu cấu hình không hợp lệ (org/role không tồn tại) thì chỉ log warning, không throw exception
 * để ứng dụng vẫn có thể start (LDAP là tính năng tùy chọn).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class LdapConfig {
    AppProperties appProperties;
    OrganizationRepository organizationRepository;
    RoleRepository roleRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void validateLdapConfig() {
        AppProperties.Ldap ldap = appProperties.getLdap();
        if (ldap == null) {
            log.warn("LDAP config is not configured in application.yml");
            return;
        }

        // Validate defaultOrgId
        String defaultOrgId = ldap.getDefaultOrgId();
        if (defaultOrgId != null && !defaultOrgId.isBlank()) {
            try {
                UUID orgId = UUID.fromString(defaultOrgId);
                Optional<OrganizationEntity> org = organizationRepository.findById(orgId);
                if (org.isEmpty()) {
                    log.warn("LDAP default-org-id '{}' does not exist in DB. Auto-assign org will be skipped.", defaultOrgId);
                } else {
                    log.info("LDAP default-org-id: '{}' ({})", defaultOrgId, org.get().getName());
                }
            } catch (IllegalArgumentException e) {
                log.warn("LDAP default-org-id '{}' is not a valid UUID", defaultOrgId);
            }
        } else {
            log.info("LDAP default-org-id is empty → auto-assign org will be skipped on first user login");
        }

        // Validate defaultRoleId
        String defaultRoleId = ldap.getDefaultRoleId();
        if (defaultRoleId != null && !defaultRoleId.isBlank()) {
            try {
                UUID roleId = UUID.fromString(defaultRoleId);
                Optional<RoleEntity> role = roleRepository.findById(roleId);
                if (role.isEmpty()) {
                    log.warn("LDAP default-role-id '{}' does not exist in DB. Will fallback to findByDefaultAssign().", defaultRoleId);
                } else {
                    log.info("LDAP default-role-id: '{}' ({})", defaultRoleId, role.get().getName());
                }
            } catch (IllegalArgumentException e) {
                log.warn("LDAP default-role-id '{}' is not a valid UUID", defaultRoleId);
            }
        } else {
            // Fallback: kiểm tra role defaultAssign có tồn tại không
            Optional<RoleEntity> defaultRole = roleRepository.findByDefaultAssign();
            if (defaultRole.isEmpty()) {
                log.warn("LDAP default-role-id is empty AND no role with defaultAssign=true found in DB");
            } else {
                log.info("LDAP default-role-id is empty → fallback to role: '{}' ({})", defaultRole.get().getId(), defaultRole.get().getName());
            }
        }

        // Validate sync config
        if (ldap.getSync() != null && ldap.getSync().isEnabled()) {
            log.info("LDAP auto-sync is ENABLED. Cron: '{}'", ldap.getSync().getCron());
            if (defaultOrgId == null || defaultOrgId.isBlank()) {
                log.warn("LDAP auto-sync is enabled but default-org-id is empty. New users will NOT be assigned an org.");
            }
        }
    }
}