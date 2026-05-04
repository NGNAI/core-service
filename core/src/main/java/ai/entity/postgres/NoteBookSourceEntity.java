package ai.entity.postgres;

import ai.entity.postgres.embeddable.AuditEmbed;
import ai.enums.DataIngestionDeleteStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

/**
 * Bảng lưu nguồn dữ liệu của notebook.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "notebook_sources", indexes = {
    @Index(name = "idx_notebook_sources_notebook_id", columnList = "notebook_id"),
    @Index(name = "idx_notebook_sources_note_id", columnList = "note_id"),
    @Index(name = "idx_notebook_sources_job_id", columnList = "job_id"),
    @Index(name = "idx_notebook_sources_delete_status", columnList = "delete_status"),
    @Index(name = "idx_notebook_sources_owner_id", columnList = "owner_id"),
    @Index(name = "idx_notebook_sources_org_id", columnList = "org_id")
})
@EntityListeners(AuditingEntityListener.class)
@Entity
public class NoteBookSourceEntity {

    public enum SourceType {
        FILE, TEXT, NOTE;
    }

    public enum VectorStatus {
        CREATED, // Mới tạo
        EXTRACTING, // Đang trích xuất
        CHUNKING, // Đang chia nhỏ
        EMBEDDING, // Đang nhúng
        STORING, // Đang lưu trữ
        COMPLETED, // Hoàn thành
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notebook_id", nullable = false)
    NoteBookEntity noteBook;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id")
    NoteEntity note;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    SourceType sourceType;

    @Column(name = "display_name", nullable = false)
    String displayName;

    @Column(name = "raw_content", columnDefinition = "TEXT", nullable = true)
    String rawContent; // Nội dung thô (dành cho sourceType = TEXT)

    @Column(name = "file_path", nullable = true)
    String filePath; // Đường dẫn file gốc (dành cho sourceType = FILE)

    @Column(name = "summary", columnDefinition = "TEXT", nullable = true)
    String summary;

    // Metadata bổ sung dạng JSON string (tag, nhãn, v.v.)
    @Column(name = "metadata", columnDefinition = "TEXT", nullable = true)
    String metadata;

    @Enumerated(EnumType.STRING)
    @Column(name = "vector_status", nullable = false)
    VectorStatus vectorStatus;

    @Column(name = "job_id")
    UUID jobId;

    @Column(name = "owner_id")
    UUID ownerId;

    @Column(name = "org_id")
    UUID organizationId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "delete_status", length = 20, nullable = false)
    DataIngestionDeleteStatus deleteStatus = DataIngestionDeleteStatus.ACTIVE;

    @Builder.Default
    @Embedded
    AuditEmbed audit = new AuditEmbed();
}
