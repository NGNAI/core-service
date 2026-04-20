package ai.entity.postgres;

import ai.entity.postgres.embeddable.TopicMessageIdEmbed;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "topic_messages")
@Entity
public class TopicMessageEntity {
    @EmbeddedId
    TopicMessageIdEmbed id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("topicId")
    TopicEntity topic;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId("messageId")
    MessageEntity message;

    public TopicMessageEntity(TopicEntity topicEntity, MessageEntity messageEntity){
        id = new TopicMessageIdEmbed(topicEntity.getId(), messageEntity.getId());
        topic = topicEntity;
        message = messageEntity;
    }
}
