package ai.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ai.entity.postgres.TopicEntity;

@Repository
public interface TopicRepository extends JpaRepository<TopicEntity, UUID>, JpaSpecificationExecutor<TopicEntity> {
    @Query("""
        SELECT COUNT(t) > 0
        FROM TopicEntity t
        WHERE t.id = :id
          AND t.owner.id = :ownerId
    """)
    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);
    
    @Query("SELECT COUNT(t) FROM TopicEntity t")
    long countAllTopics();
    
    @Query("SELECT COUNT(t) FROM TopicEntity t WHERE t.organization.id = :orgId")
    long countAllTopicsByOrgId(UUID orgId);

    // Count topics by updated date (day precision)
    @Query(value = "SELECT COUNT(t) FROM topic t WHERE DATE(t.updated_at) = :date", nativeQuery = true)
    long countTopicsByDate(java.time.LocalDate date);

    // Count topics by updated date and organization
    @Query(value = "SELECT COUNT(t) FROM topic t WHERE DATE(t.updated_at) = :date AND t.organization_id = :orgId", nativeQuery = true)
    long countTopicsByDateAndOrgId(java.time.LocalDate date, UUID orgId);
}
