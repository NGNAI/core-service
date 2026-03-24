package ai.entity.postgres;

import ai.entity.postgres.embeddable.AuditEmbed;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "topic")
@EntityListeners(AuditingEntityListener.class)
@Entity
public class TopicEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    UUID id;

    @Column(name = "title", nullable = false)
    String title;

    @Column(name = "type", nullable = false)
    String type;

    @Embedded
    AuditEmbed audit= new AuditEmbed();

    @ManyToOne
    @JoinColumn(name = "owner_id")
    UserEntity owner;

    @OneToMany(mappedBy = "topic", cascade = CascadeType.ALL)
    List<MessageEntity> messages;
}
