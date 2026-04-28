package ai.entity.postgres;

import ai.entity.postgres.embeddable.AuditEmbed;
import ai.enums.NoteSourceType;
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
@Table(name = "note", indexes = {
        @Index(name = "idx_note_owner_id", columnList = "owner_id"),
        @Index(name = "idx_note_source_type", columnList = "source_type"),
        @Index(name = "idx_note_topic_id", columnList = "topic_id"),
        @Index(name = "idx_note_notebook_id", columnList = "notebook_id")
})
@EntityListeners(AuditingEntityListener.class)
@Entity
public class NoteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    UUID id;

    @Column(name = "title")
    String title;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    NoteSourceType sourceType;

    @Column(name = "topic_id")
    UUID topicId;

    @Column(name = "notebook_id")
    UUID noteBookId;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    UserEntity owner;

    @Embedded
    AuditEmbed audit = new AuditEmbed();
}
