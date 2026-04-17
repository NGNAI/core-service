package ai.repository;

import ai.entity.postgres.TopicFileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TopicFileRepository extends JpaRepository<TopicFileEntity, UUID> {

    Page<TopicFileEntity> findByTopicId(UUID topicId, Pageable pageable);

    boolean existsByTopicIdAndDataIngestionId(UUID topicId, UUID dataIngestionId);

    Optional<TopicFileEntity> findByTopicIdAndDataIngestionId(UUID topicId, UUID dataIngestionId);

    @Query("SELECT COUNT(tf) FROM TopicFileEntity tf WHERE tf.topic.id = :topicId")
    long countByTopicId(UUID topicId);
}
