package ai.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import ai.entity.postgres.NotebookMessageEntity;
import ai.entity.postgres.embeddable.NoteBookMessageIdEmbed;

@Repository
public interface NotebookMessagesRepository extends JpaRepository<NotebookMessageEntity, NoteBookMessageIdEmbed>, JpaSpecificationExecutor<NotebookMessageEntity> {
	List<NotebookMessageEntity> findByNotebook_IdOrderById_MessageIdAsc(UUID notebookId);

	List<NotebookMessageEntity> findByNotebook_IdAndId_MessageIdGreaterThanOrderById_MessageIdAsc(UUID notebookId, UUID messageId);

	boolean existsByNotebook_IdAndMessage_Id(UUID notebookId, UUID messageId);
}
