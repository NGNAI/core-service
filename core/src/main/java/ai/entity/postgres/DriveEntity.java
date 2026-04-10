package ai.entity.postgres;

import java.util.UUID;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import ai.entity.postgres.embeddable.AuditEmbed;
import ai.enums.DataIngestionDeleteStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "drive", indexes = {
        @jakarta.persistence.Index(name = "idx_drive_folder", columnList = "folder"),
        @jakarta.persistence.Index(name = "idx_drive_org_id", columnList = "org_id"),
        @jakarta.persistence.Index(name = "idx_drive_owner_id", columnList = "owner_id"),
        @jakarta.persistence.Index(name = "idx_drive_parent_id", columnList = "parent_id"),
        @jakarta.persistence.Index(name = "idx_drive_delete_status", columnList = "delete_status")
})
@EntityListeners(AuditingEntityListener.class)
@Entity
public class DriveEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    UUID id;

    @Column(name = "name", nullable = false)
    String name;

    @Column(name = "folder", nullable = false)
    boolean folder;

    @Column(name = "content_type", length = 120)
    String contentType;

    @Column(name = "file_size")
    Long fileSize;

    @Column(name = "minio_path")
    String minioPath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    DriveEntity parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    UserEntity owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id")
    OrganizationEntity organization;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "delete_status", length = 20, nullable = false)
    DataIngestionDeleteStatus deleteStatus = DataIngestionDeleteStatus.ACTIVE;

    @Builder.Default
    @Embedded
    AuditEmbed audit = new AuditEmbed();
}
