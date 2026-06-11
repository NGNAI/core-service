package ai.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import ai.entity.postgres.DraftMessageEntity;
import ai.entity.postgres.embeddable.DraftMessageIdEmbed;

@Repository
public interface DraftMessagesRepository
        extends JpaRepository<DraftMessageEntity, DraftMessageIdEmbed>,
                JpaSpecificationExecutor<DraftMessageEntity> {

    List<DraftMessageEntity> findByDraft_IdOrderById_MessageIdAsc(UUID draftId);

    List<DraftMessageEntity> findByDraft_IdAndId_MessageIdGreaterThanOrderById_MessageIdAsc(UUID draftId,
            UUID messageId);

    boolean existsByDraft_IdAndMessage_Id(UUID draftId, UUID messageId);
}
