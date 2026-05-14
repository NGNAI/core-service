package ai.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import ai.entity.postgres.NoteEntity;

@Repository
public interface NoteRepository extends JpaRepository<NoteEntity, UUID>, JpaSpecificationExecutor<NoteEntity> {
    boolean existsByIdAndOwnerId(UUID noteId, UUID ownerId);

    public boolean existsByIdAndOrganizationId(UUID noteId, UUID organizationId);
}
