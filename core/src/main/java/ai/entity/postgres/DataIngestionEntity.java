package ai.entity.postgres;

import java.util.UUID;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import ai.entity.postgres.embeddable.AuditEmbed;
import ai.enums.DataIngestionDeleteStatus;
import ai.enums.DataScope;
import ai.enums.IngestionStatus;
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
@Table(name = "data_ingestion", indexes = {
        @jakarta.persistence.Index(name = "idx_data_ingestion_folder", columnList = "folder"),
        @jakarta.persistence.Index(name = "idx_data_ingestion_org_id", columnList = "org_id"),
        @jakarta.persistence.Index(name = "idx_data_ingestion_owner_id", columnList = "owner_id"),
        @jakarta.persistence.Index(name = "idx_data_ingestion_parent_id", columnList = "parent_id"),
        @jakarta.persistence.Index(name = "idx_data_ingestion_target", columnList = "target"),
        @jakarta.persistence.Index(name = "idx_data_ingestion_delete_status", columnList = "delete_status")
})
@EntityListeners(AuditingEntityListener.class)
@Entity
public class DataIngestionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    UUID id;

    // Tên file hoặc tên folder
    @Column(name = "name", nullable = false)
    String name;

    // Nếu data ingestion này là folder thì trường folder sẽ là true, nếu là file thì trường folder sẽ là false
    @Column(name = "folder", nullable = false)
    boolean folder;

    // Loại file, ví dụ: "image/png", "application/pdf", v.v. Null nếu data ingestion này là folder
    @Column(name = "content_type", length = 50)
    String contentType;

    // Kích thước của file, null nếu data ingestion này là folder
    @Column(name = "file_size")
    Long fileSize;

    // Đường dẫn lưu file trong MinIO, có thể là đường dẫn tuyệt đối hoặc đường dẫn tương đối tùy theo cách triển khai MinIO
    @Column(name = "minio_path")
    String minioPath;

    // Nếu data ingestion này dùng chung cho cả tổ chức thì trường organization sẽ không null, nếu là dữ liệu cá nhân thì organization sẽ null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    DataIngestionEntity parent;

    // Nếu data ingestion này thuộc cá nhân thì owner sẽ không null; nếu là dữ liệu chia sẻ theo tổ chức thì owner có thể null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    UserEntity owner;

    // Nếu data ingestion này là dữ liệu dùng chung cho cả tổ chức thì organization sẽ không null, nếu là dữ liệu cá nhân thì organization có thể null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id")
    OrganizationEntity organization;

    // Quyền truy cập của data ingestion này, có thể là PERSONAL, LOCAL hoặc GLOBAL
    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", length = 20)
    DataScope accessLevel;

    // ID của ingestion job nếu data ingestion này được upload để ingest, null nếu không liên quan đến ingestion
    @Column(name = "job_id", nullable = true)
    UUID jobId;

    // Trạng thái của ingestion job, null nếu data ingestion này không liên quan đến ingestion
    @Enumerated(EnumType.STRING)
    @Column(name = "ingestion_status", length = 20, nullable = true)
    IngestionStatus ingestionStatus;

    // Trạng thái xóa của data ingestion, ACTIVE nếu đang sử dụng bình thường, DELETED nếu đã bị xóa (có thể là xóa mềm hoặc xóa cứng tùy theo cách triển khai)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "delete_status", length = 20, nullable = false)
    DataIngestionDeleteStatus deleteStatus = DataIngestionDeleteStatus.ACTIVE;

    @Builder.Default
    @Embedded
    AuditEmbed audit = new AuditEmbed();
}
