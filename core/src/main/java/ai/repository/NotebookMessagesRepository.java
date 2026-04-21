package ai.repository;

import ai.entity.postgres.NotebookMessageEntity;
import ai.entity.postgres.TopicMessageEntity;
import ai.entity.postgres.embeddable.TopicMessageIdEmbed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface NotebookMessagesRepository extends JpaRepository<NotebookMessageEntity, TopicMessageIdEmbed>, JpaSpecificationExecutor<NotebookMessageEntity> {
}
