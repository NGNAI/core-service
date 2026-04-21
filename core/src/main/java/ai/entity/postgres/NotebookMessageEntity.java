package ai.entity.postgres;

import ai.entity.postgres.embeddable.NoteBookMessageIdEmbed;
import ai.interfaces.MessageRelationEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "notebook_messages")
@Entity
public class NotebookMessageEntity implements MessageRelationEntity {
    @EmbeddedId
    NoteBookMessageIdEmbed id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("notebookId")
    NoteBookEntity notebook;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @MapsId("messageId")
    MessageEntity message;

    public NotebookMessageEntity(NoteBookEntity notebookEntity, MessageEntity messageEntity){
        id = new NoteBookMessageIdEmbed(notebookEntity.getId(), messageEntity.getId());
        notebook = notebookEntity;
        message = messageEntity;
    }

    @Override
    public MessageEntity getMessageEntity() {
        return message;
    }
}
