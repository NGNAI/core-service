package ai.repository;

import ai.entity.postgres.TopicEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TopicRepository extends JpaRepository<TopicEntity, UUID>, JpaSpecificationExecutor<TopicEntity> {
    @Query("""
        SELECT COUNT(t) > 0
        FROM TopicEntity t
        WHERE t.id = :id
          AND t.owner.id = :ownerId
    """)
    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);
}
