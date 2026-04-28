package ai.repository;

import ai.entity.postgres.NoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NoteRepository extends JpaRepository<NoteEntity, UUID>, JpaSpecificationExecutor<NoteEntity> {
    boolean existsByIdAndOwnerId(UUID noteId, UUID ownerId);
}
