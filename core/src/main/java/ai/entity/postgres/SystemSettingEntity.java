package ai.entity.postgres;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import ai.entity.postgres.embeddable.AuditEmbed;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "system_settings", uniqueConstraints = {
        @UniqueConstraint(columnNames = "setting_key", name = "uk_system_settings_key")
})
@EntityListeners(AuditingEntityListener.class)
@Entity
public class SystemSettingEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    UUID id;

    @Column(name = "setting_key", nullable = false, unique = true, length = 128)
    String key;

    @Column(name = "setting_value", columnDefinition = "TEXT")
    String value;

    @Column(name = "description", length = 512)
    String description;

    @Column(name = "setting_type", nullable = false, length = 32)
    @Builder.Default
    String type = "STRING";

    @Column(name = "setting_group", nullable = false, length = 64)
    @Builder.Default
    String groupName = "GENERAL";

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    Boolean isPublic = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    Boolean isActive = true;

    @Column(name = "display_order")
    @Builder.Default
    Integer displayOrder = 0;

    @Builder.Default
    @Embedded
    AuditEmbed audit = new AuditEmbed();
}
