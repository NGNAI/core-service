package ai.entity.postgres;

import java.util.UUID;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import ai.entity.postgres.embeddable.AuditEmbed;
import ai.enums.DataScope;
import ai.enums.IngestionStatus;
import ai.enums.MediaUploadTarget;
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
@Table(name = "media", indexes = {
        @jakarta.persistence.Index(name = "idx_media_folder", columnList = "folder"),
        @jakarta.persistence.Index(name = "idx_media_org_id", columnList = "org_id"),
        @jakarta.persistence.Index(name = "idx_media_owner_id", columnList = "owner_id"),
        @jakarta.persistence.Index(name = "idx_media_parent_id", columnList = "parent_id"),
        @jakarta.persistence.Index(name = "idx_media_target", columnList = "target")
})
@EntityListeners(AuditingEntityListener.class)
@Entity
public class MediaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    UUID id;

    // Tên file hoặc tên folder
    @Column(name = "name", nullable = false)
    String name;

    // Nếu media này là folder thì trường folder sẽ là true, nếu media này là file thì trường folder sẽ là false
    @Column(name = "folder", nullable = false)
    boolean folder;

    // Loại file, ví dụ: "image/png", "application/pdf", v.v. Null nếu media này là folder
    @Column(name = "content_type", length = 50)
    String contentType;

    // Kích thước của file, null nếu media này là folder
    @Column(name = "file_size")
    Long fileSize;

    // Đường dẫn lưu file trong MinIO, có thể là đường dẫn tuyệt đối hoặc đường dẫn tương đối tùy theo cách triển khai MinIO
    @Column(name = "minio_path")
    String minioPath;

    // Nếu media này là media dùng chung cho cả tổ chức thì trường organization sẽ không null, nếu media này là media cá nhân thì trường organization sẽ null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    MediaEntity parent;

    // Nếu media này là media dùng chung cho cả tổ chức thì trường owner sẽ không null, nếu media này là media cá nhân thì trường owner sẽ null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    UserEntity owner;

    // Nếu media này là media dùng chung cho cả tổ chức thì trường organization sẽ không null, nếu media này là media cá nhân thì trường organization sẽ null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id")
    OrganizationEntity organization;

    // Quyền truy cập của media này, có thể là PERSONAL (chỉ owner có quyền truy cập), LOCAL (owner và các thành viên trong cùng tổ chức có quyền truy cập), hoặc GLOBAL (tất cả mọi người đều có quyền truy cập)
    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", length = 20)
    DataScope accessLevel;

    // Mục đích của media này, dùng để làm gì, liên quan đến entity nào, v.v. Ví dụ: nếu media này dùng để làm avatar cho User thì target sẽ là AVATAR, nếu media này dùng để đính kèm cho Topic thì target sẽ là TOPIC, v.v.
    @Enumerated(EnumType.STRING)
    @Column(name = "target", length = 20)
    MediaUploadTarget target;

    // ID của ingestion job nếu media này được upload để ingest, null nếu media này không liên quan đến ingestion
    @Column(name = "job_id", nullable = true)
    UUID jobId;

    // Trạng thái của ingestion job, null nếu media này không liên quan đến ingestion
    @Enumerated(EnumType.STRING)
    @Column(name = "ingestion_status", length = 20, nullable = true)
    IngestionStatus ingestionStatus;

    @Builder.Default
    @Embedded
    AuditEmbed audit = new AuditEmbed();
}
