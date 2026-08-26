package ai.entity.postgres;

import ai.entity.postgres.embeddable.AuditEmbed;
import ai.enums.FeedbackStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "feedbacks", indexes = {
        @Index(name = "idx_feedbacks_sender_id", columnList = "sender_id"),
        @Index(name = "idx_feedbacks_status", columnList = "status"),
        @Index(name = "idx_feedbacks_org_id", columnList = "sender_org_id"),
        @Index(name = "idx_feedbacks_created_at", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Entity
public class FeedbackEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    UUID id;

    @Column(name = "subject", nullable = false, length = 255)
    String subject;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    String content;

    @Column(name = "is_private", nullable = false)
    @Builder.Default
    Boolean isPrivate = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    FeedbackStatus status = FeedbackStatus.PENDING;

    @Column(name = "response_content", columnDefinition = "TEXT")
    String responseContent;

    @Column(name = "response_date")
    Instant responseDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    UserEntity sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_org_id", nullable = false)
    OrganizationEntity senderOrg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responder_id")
    UserEntity responder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responder_org_id")
    OrganizationEntity responderOrg;

    @Embedded
    AuditEmbed audit = new AuditEmbed();
}
