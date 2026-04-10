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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "attachment", indexes = {
        @jakarta.persistence.Index(name = "idx_attachment_topic_id", columnList = "topic_id"),
        @jakarta.persistence.Index(name = "idx_attachment_message_id", columnList = "message_id"),
        @jakarta.persistence.Index(name = "idx_attachment_org_id", columnList = "org_id"),
        @jakarta.persistence.Index(name = "idx_attachment_owner_id", columnList = "owner_id")
})
@EntityListeners(AuditingEntityListener.class)
@Entity
public class AttachmentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    UUID id;

    @Column(name = "name", nullable = false)
    String name;

    @Column(name = "minio_path", nullable = false)
    String minioPath;

    @Column(name = "file_size", nullable = false)
    Long fileSize;

    @Column(name = "content_type", length = 120)
    String contentType;

    @Column(name = "topic_id")
    UUID topicId;

    @Column(name = "message_id")
    UUID messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    UserEntity owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    OrganizationEntity organization;

    @Builder.Default
    @Embedded
    AuditEmbed audit = new AuditEmbed();
}
