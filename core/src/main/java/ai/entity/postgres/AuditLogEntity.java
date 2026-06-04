package ai.entity.postgres;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import ai.enums.AuditAction;
import ai.enums.AuditResource;
import ai.enums.AuditStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_log_user", columnList = "user_id"),
        @Index(name = "idx_audit_log_org", columnList = "org_id"),
        @Index(name = "idx_audit_log_action", columnList = "action"),
        @Index(name = "idx_audit_log_resource", columnList = "resource"),
        @Index(name = "idx_audit_log_created_at", columnList = "created_at"),
        @Index(name = "idx_audit_log_status", columnList = "status")
})
@Entity
public class AuditLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 64)
    AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource", nullable = false, length = 64)
    AuditResource resource;

    @Column(name = "resource_id", length = 128)
    String resourceId;

    @Column(name = "resource_name", length = 512)
    String resourceName;

    @Column(name = "description", length = 1024)
    String description;

    @Column(name = "user_id")
    UUID userId;

    @Column(name = "user_name", length = 256)
    String userName;

    @Column(name = "org_id")
    UUID orgId;

    @Column(name = "organization_name", length = 256)
    String organizationName;

    @Column(name = "ip_address", length = 64)
    String ipAddress;

    @Column(name = "user_agent", length = 512)
    String userAgent;

    @Column(name = "method", length = 128)
    String method;

    @Column(name = "path", length = 512)
    String path;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    AuditStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb")
    Map<String, Object> details;

    @Column(name = "error_message", length = 2048)
    String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
