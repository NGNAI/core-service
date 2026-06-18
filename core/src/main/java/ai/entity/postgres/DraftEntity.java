package ai.entity.postgres;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import ai.entity.postgres.embeddable.AuditEmbed;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "draft", indexes = {
        @Index(name = "idx_draft_owner_id", columnList = "owner_id"),
        @Index(name = "idx_draft_org_id", columnList = "organization_id"),
        @Index(name = "idx_draft_updated_at", columnList = "updated_at")
})
@EntityListeners(AuditingEntityListener.class)
@Entity
public class DraftEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    UUID id;

    @Column(name = "type", nullable = false)
    String type;

    @Column(name = "title", nullable = false)
    String title;

    @Column(name = "detailed_description", nullable = true, columnDefinition = "TEXT")
    String detailedDescription;

    @Column(name = "latest_version_number", nullable = false)
    Integer latestVersionNumber;

    @Column(name = "latest_content_preview", columnDefinition = "TEXT")
    String latestContentPreview;

    @Embedded
    AuditEmbed audit = new AuditEmbed();

    @ManyToOne
    @JoinColumn(name = "owner_id")
    UserEntity owner;

    @ManyToOne
    @JoinColumn(name = "organization_id")
    OrganizationEntity organization;

    @OneToMany(mappedBy = "draft")
    List<DraftVersionEntity> versions;

    @Column(name = "session_id", nullable = true)
    String sessionId;
}
