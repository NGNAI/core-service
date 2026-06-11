package ai.entity.postgres;

import ai.entity.postgres.embeddable.DraftMessageIdEmbed;
import ai.interfaces.MessageRelationEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "draft_messages")
@Entity
public class DraftMessageEntity implements MessageRelationEntity {
    @EmbeddedId
    DraftMessageIdEmbed id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("draftId")
    DraftEntity draft;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @MapsId("messageId")
    MessageEntity message;

    public DraftMessageEntity(DraftEntity draftEntity, MessageEntity messageEntity) {
        id = new DraftMessageIdEmbed(draftEntity.getId(), messageEntity.getId());
        draft = draftEntity;
        message = messageEntity;
    }

    @Override
    public MessageEntity getMessageEntity() {
        return message;
    }
}
