package ai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import ai.entity.postgres.NotebookMessageEntity;
import ai.entity.postgres.embeddable.NoteBookMessageIdEmbed;

@Repository
public interface NotebookMessagesRepository extends JpaRepository<NotebookMessageEntity, NoteBookMessageIdEmbed>, JpaSpecificationExecutor<NotebookMessageEntity> {
}
