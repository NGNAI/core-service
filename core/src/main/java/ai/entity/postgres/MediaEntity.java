package ai.entity.postgres;

import ai.entity.postgres.embeddable.AuditEmbed;
import ai.enums.MediaUploadTarget;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "media")
@EntityListeners(AuditingEntityListener.class)
@Entity
public class MediaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    UUID id;

    @Column(name = "name", nullable = false)
    String name;

    @Column(name = "type", length = 50)
    String type;

    @Column(name = "size")
    Long size;

    @Column(name = "minio_path")
    String minioPath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    MediaEntity parent;

    @Column(name = "owner_id")
    UUID ownerId;

    @Column(name = "org_id")
    UUID orgId;

    @Column(name = "access_level", length = 20)
    String accessLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "target", length = 20)
    MediaUploadTarget target;

    @Column(name = "job_id")
    UUID jobId;

    @Column(name = "ingestion_status", length = 20)
    String ingestionStatus;

    @Column(name = "download_count")
    Integer downloadCount = 0;

    @Embedded
    AuditEmbed audit = new AuditEmbed();
}
