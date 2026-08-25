package ai.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ai.entity.postgres.FeedbackEntity;
import ai.enums.FeedbackStatus;

@Repository
public interface FeedbackRepository extends JpaRepository<FeedbackEntity, UUID>, JpaSpecificationExecutor<FeedbackEntity> {
    
    // User queries - user's own feedbacks
    List<FeedbackEntity> findBySenderId(UUID userId);
    
    List<FeedbackEntity> findBySenderOrgId(UUID orgId);
    
    List<FeedbackEntity> findByStatus(FeedbackStatus status);
    
    // Admin queries - all feedbacks
    List<FeedbackEntity> findAllByStatusIn(Collection<FeedbackStatus> statuses);
    
    @Query("SELECT f FROM FeedbackEntity f WHERE LOWER(f.subject) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(f.content) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<FeedbackEntity> searchByKeyword(@Param("keyword") String keyword);
    
    @Query("SELECT f FROM FeedbackEntity f WHERE f.senderOrg.id IN :orgIds")
    List<FeedbackEntity> findAllBySenderOrgIds(@Param("orgIds") Collection<UUID> orgIds);
    
    @Query("SELECT COUNT(f) FROM FeedbackEntity f WHERE f.status = ai.enums.FeedbackStatus.PENDING")
    long countPendingFeedbacks();

    // Custom validation methods
    boolean existsByIdAndSenderId(UUID id, UUID senderId);
}
