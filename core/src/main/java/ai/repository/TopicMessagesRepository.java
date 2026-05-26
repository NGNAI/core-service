package ai.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import ai.entity.postgres.TopicMessageEntity;
import ai.entity.postgres.embeddable.TopicMessageIdEmbed;

@Repository
public interface TopicMessagesRepository extends JpaRepository<TopicMessageEntity, TopicMessageIdEmbed>, JpaSpecificationExecutor<TopicMessageEntity> {
	List<TopicMessageEntity> findByTopic_IdOrderById_MessageIdAsc(UUID topicId);

	List<TopicMessageEntity> findByTopic_IdAndId_MessageIdGreaterThanOrderById_MessageIdAsc(UUID topicId, UUID messageId);
}
