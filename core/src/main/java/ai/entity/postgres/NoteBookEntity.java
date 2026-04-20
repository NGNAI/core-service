package ai.entity.postgres;

import ai.entity.postgres.embeddable.AuditEmbed;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "notebook")
@EntityListeners(AuditingEntityListener.class)
@Entity
public class NoteBookEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    UUID id;

    @Column(name = "title", nullable = false)
    String title;

    @Column(name = "instruction", nullable = false)
    String instruction;

    @Embedded
    AuditEmbed audit= new AuditEmbed();

    @ManyToOne
    @JoinColumn(name = "owner_id")
    UserEntity owner;

    @OneToMany(mappedBy = "noteBook", cascade = CascadeType.ALL, orphanRemoval = true)
    List<NoteBookFileEntity> noteBookFiles;
}
