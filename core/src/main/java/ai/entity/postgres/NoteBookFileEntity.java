package ai.entity.postgres;

import ai.entity.postgres.embeddable.AuditEmbed;
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
 * Bảng trung gian liên kết NoteBook với DataIngestion (file/folder được gắn vào notebook).
 * Khoá chính ghép từ notebook + dataIngestion để đảm bảo mỗi cặp chỉ xuất hiện một lần.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "notebook_file", indexes = {
        @Index(name = "idx_notebook_file_notebook_id", columnList = "notebook_id"),
        @Index(name = "idx_notebook_file_data_ingestion_id", columnList = "data_ingestion_id")
})
@EntityListeners(AuditingEntityListener.class)
@Entity
public class NoteBookFileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notebook_id", nullable = false)
    NoteBookEntity noteBook;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "data_ingestion_id", nullable = false)
    DataIngestionEntity dataIngestion;

    // Tóm tắt nội dung file trong ngữ cảnh notebook này
    @Column(name = "summary", columnDefinition = "TEXT")
    String summary;

    // Metadata bổ sung dạng JSON string (tag, nhãn, v.v.)
    @Column(name = "metadata", columnDefinition = "TEXT")
    String metadata;

    @Builder.Default
    @Embedded
    AuditEmbed audit = new AuditEmbed();
}
