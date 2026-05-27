package ai.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ai.entity.postgres.MessageFeedbackHistoryEntity;

@Repository
public interface MessageFeedbackHistoryRepository extends JpaRepository<MessageFeedbackHistoryEntity, UUID> {
	Page<MessageFeedbackHistoryEntity> findByMessage_IdOrderByAudit_CreatedAtDesc(UUID messageId, Pageable pageable);
}
