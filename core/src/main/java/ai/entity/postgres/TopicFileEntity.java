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
 * Bảng trung gian liên kết Topic với DataIngestion (file/folder được gắn vào topic).
 * Khoá chính ghép từ topic + dataIngestion để đảm bảo mỗi cặp chỉ xuất hiện một lần.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "topic_file", indexes = {
        @Index(name = "idx_topic_file_topic_id", columnList = "topic_id"),
    @Index(name = "idx_topic_file_data_ingestion_id", columnList = "data_ingestion_id"),
    @Index(name = "idx_topic_file_message_id", columnList = "message_id")
})
@EntityListeners(AuditingEntityListener.class)
@Entity
public class TopicFileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    TopicEntity topic;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "data_ingestion_id", nullable = false)
    DataIngestionEntity dataIngestion;

    // Tóm tắt nội dung file trong ngữ cảnh topic này
    @Column(name = "summary", columnDefinition = "TEXT")
    String summary;

    // Metadata bổ sung dạng JSON string (tag, nhãn, v.v.)
    @Column(name = "metadata", columnDefinition = "TEXT")
    String metadata;

    @Column(name = "message_id")
    UUID messageId;

    @Builder.Default
    @Embedded
    AuditEmbed audit = new AuditEmbed();
}
