package ai.entity.postgres;

import java.util.UUID;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import ai.entity.postgres.embeddable.AuditEmbed;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "draft_version", indexes = {
        @Index(name = "idx_draft_version_draft_id", columnList = "draft_id"),
        @Index(name = "idx_draft_version_created_at", columnList = "created_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_draft_version_draft_id_version_number", columnNames = { "draft_id", "version_number" })
})
@EntityListeners(AuditingEntityListener.class)
@Entity
public class DraftVersionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "draft_id", nullable = false)
    DraftEntity draft;

    @Column(name = "version_number", nullable = false)
    Integer versionNumber;

    @Column(name = "detailed_description", nullable = false, columnDefinition = "TEXT")
    String detailedDescription;

    @Column(name = "change_request", nullable = true, columnDefinition = "TEXT")
    String changeRequest;

    @Column(name = "generated_content", nullable = false, columnDefinition = "TEXT")
    String generatedContent;

    @Embedded
    AuditEmbed audit = new AuditEmbed();
}
