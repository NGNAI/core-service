package ai.entity.postgres;

import ai.entity.postgres.embeddable.AuditEmbed;
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
@Table(name = "message")
@EntityListeners(AuditingEntityListener.class)
@Entity
public class MessageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    UUID id;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    String content;

    @Column(name = "type", nullable = false)
    String type;

    @Column(name = "source", columnDefinition = "TEXT")
    String source;

    @Embedded
    AuditEmbed audit= new AuditEmbed();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    TopicEntity topic;
}
