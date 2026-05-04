package ai.entity.postgres;

import ai.entity.postgres.embeddable.AuditEmbed;
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
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "topic_sources", indexes = {
    @Index(name = "idx_topic_sources_topic_id", columnList = "topic_id")
})
@EntityListeners(AuditingEntityListener.class)
@Entity
public class TopicSourceEntity {

    public enum SourceType {
        FILE
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
    @JoinColumn(name = "topic_id", nullable = false)
    TopicEntity topic;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    SourceType sourceType;

    @Column(name = "display_name", nullable = false)
    String displayName;

    @Column(name = "raw_content", columnDefinition = "TEXT")
    String rawContent;

    @Column(name = "file_path")
    String filePath;

    @Column(name = "summary", columnDefinition = "TEXT")
    String summary;

    @Column(name = "metadata", columnDefinition = "TEXT")
    String metadata;

    @Enumerated(EnumType.STRING)
    @Column(name = "vector_status", nullable = false)
    VectorStatus vectorStatus;

    @Builder.Default
    @Embedded
    AuditEmbed audit = new AuditEmbed();
}
