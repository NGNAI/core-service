package ai.entity.postgres;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import ai.entity.postgres.embeddable.AuditEmbed;
import ai.enums.ShareResource;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * Lưu public share link cho Topic / Notebook.
 * <p>
 * Cơ chế bảo mật link:
 * <ul>
 *   <li>{@code token} — 32 byte ngẫu nhiên mã hóa base62 (URL-safe), không đánh đoán được.</li>
 *   <li>{@code passwordHash} — BCrypt hash, {@code null} = không yêu cầu password.</li>
 *   <li>{@code expiresAt} — {@code null} = vĩnh viễn.</li>
 *   <li>{@code revokedAt} — {@code null} = còn hiệu lực, không null = đã bị hủy.</li>
 * </ul>
 * Viewer truy cập qua {@code /public/share/{token}} (read-only) — không cần JWT.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "share_links", indexes = {
        @Index(name = "idx_share_links_token", columnList = "token", unique = true),
        @Index(name = "idx_share_links_resource", columnList = "resource_type,resource_id"),
        @Index(name = "idx_share_links_owner", columnList = "owner_id")
})
@EntityListeners(AuditingEntityListener.class)
@Entity
public class ShareLinkEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    UUID id;

    /**
     * Token ngẫu nhiên 32 byte base62 (URL-safe, ~43 ký tự). Unique.
     * Sinh bằng {@link java.security.SecureRandom}, không đánh đoán được.
     */
    @Column(name = "token", nullable = false, unique = true, updatable = false, length = 64)
    String token;

    /**
     * Loại tài nguyên được share: TOPIC hoặc NOTEBOOK.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 32)
    ShareResource resourceType;

    /**
     * UUID của TopicEntity / NoteBookEntity được share.
     */
    @Column(name = "resource_id", nullable = false)
    UUID resourceId;

    /**
     * UUID của UserEntity tạo link (owner).
     */
    @Column(name = "owner_id", nullable = false)
    UUID ownerId;

    /**
     * UUID của OrganizationEntity mà owner thuộc vào lúc tạo link.
     */
    @Column(name = "organization_id", nullable = false)
    UUID organizationId;

    /**
     * Mô tả link do owner đặt (vd "Gửi cho team A"). Nullable.
     */
    @Column(name = "title", length = 256)
    String title;

    /**
     * BCrypt hash của password do owner đặt. {@code null} = không yêu cầu password.
     */
    @Column(name = "password_hash", length = 100)
    String passwordHash;

    /**
     * Thời điểm link hết hạn. {@code null} = vĩnh viễn.
     */
    @Column(name = "expires_at")
    Instant expiresAt;

    /**
     * Thời điểm link bị owner hủy (revoke). {@code null} = còn hiệu lực.
     */
    @Column(name = "revoked_at")
    Instant revokedAt;

    /**
     * Tổng số lượt xem trên link này (tăng dần qua mỗi request public hợp lệ).
     */
    @Builder.Default
    @Column(name = "view_count", nullable = false)
    long viewCount = 0L;

    /**
     * Thời điểm xem gần nhất.
     */
    @Column(name = "last_viewed_at")
    Instant lastViewedAt;

    @Builder.Default
    @Embedded
    AuditEmbed audit = new AuditEmbed();

    /**
     * Kiểm tra link đã bị hủy (revoke) chưa.
     */
    public boolean isRevoked() {
        return revokedAt != null;
    }

    /**
     * Kiểm tra link đã hết hạn chưa.
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    /**
     * Kiểm tra link có yêu cầu password không.
     */
    public boolean isPasswordRequired() {
        return passwordHash != null && !passwordHash.isBlank();
    }
}