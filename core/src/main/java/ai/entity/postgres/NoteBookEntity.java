package ai.entity.postgres;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import ai.entity.postgres.embeddable.AuditEmbed;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

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

    @Column(name = "description", nullable = false)
    String description;

    @Column(name = "instruction")
    String instruction;

    @Embedded
    AuditEmbed audit= new AuditEmbed();

    @ManyToOne
    @JoinColumn(name = "owner_id")
    UserEntity owner;

    @OneToMany(mappedBy = "noteBook", cascade = CascadeType.ALL, orphanRemoval = true)
    List<NoteBookFileEntity> noteBookFiles;
}
