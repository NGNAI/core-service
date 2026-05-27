package ai.entity.postgres;

import java.util.UUID;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import ai.entity.postgres.embeddable.AuditEmbed;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "message_feedback_history", indexes = {
        @Index(name = "idx_message_feedback_history_message_id", columnList = "message_id"),
        @Index(name = "idx_message_feedback_history_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Entity
public class MessageFeedbackHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    MessageEntity message;

    @Column(name = "before_feedback")
    String beforeFeedback;

    @Column(name = "after_feedback")
    String afterFeedback;

    @Embedded
    AuditEmbed audit = new AuditEmbed();
}
