package ai.entity.postgres;

import java.util.UUID;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import ai.annotation.UuidV7;
import ai.entity.postgres.embeddable.AuditEmbed;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "message")
@EntityListeners(AuditingEntityListener.class)
@Entity
public class MessageEntity {
    @Id
    @UuidV7
    @Column(name = "id", nullable = false)
    UUID id;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    String content;

    @Column(name = "type", nullable = false)
    String type;

    @Column(name = "source", columnDefinition = "TEXT")
    String source;

    @Column(name = "feedback")
    String feedback;

    @Embedded
    AuditEmbed audit= new AuditEmbed();
}
