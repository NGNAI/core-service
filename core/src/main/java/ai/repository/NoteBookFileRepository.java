package ai.repository;

import ai.entity.postgres.NoteBookFileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NoteBookFileRepository extends JpaRepository<NoteBookFileEntity, UUID> {

    Page<NoteBookFileEntity> findByNoteBookId(UUID noteBookId, Pageable pageable);

    boolean existsByNoteBookIdAndDataIngestionId(UUID noteBookId, UUID dataIngestionId);

    Optional<NoteBookFileEntity> findByNoteBookIdAndDataIngestionId(UUID noteBookId, UUID dataIngestionId);

    @Query("SELECT COUNT(nf) FROM NoteBookFileEntity nf WHERE nf.noteBook.id = :noteBookId")
    long countByNoteBookId(UUID noteBookId);
}
