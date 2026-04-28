package ai.repository;

import ai.entity.postgres.TopicSourceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TopicSourceRepository extends JpaRepository<TopicSourceEntity, UUID> {

    Page<TopicSourceEntity> findByTopicId(UUID topicId, Pageable pageable);

    List<TopicSourceEntity> findByTopicId(UUID topicId);

    boolean existsByTopicIdAndDisplayNameAndSourceType(UUID topicId, String displayName, TopicSourceEntity.SourceType sourceType);

    Optional<TopicSourceEntity> findByTopicIdAndId(UUID topicId, UUID sourceId);

    @Query("SELECT COUNT(ts) FROM TopicSourceEntity ts WHERE ts.topic.id = :topicId")
    long countByTopicId(UUID topicId);
}
