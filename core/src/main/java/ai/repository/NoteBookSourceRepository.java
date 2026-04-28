package ai.repository;

import ai.entity.postgres.NoteBookSourceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NoteBookSourceRepository extends JpaRepository<NoteBookSourceEntity, UUID> {

    Page<NoteBookSourceEntity> findByNoteBookId(UUID noteBookId, Pageable pageable);

    boolean existsByNoteBookIdAndDisplayNameAndSourceType(UUID noteBookId, String displayName, NoteBookSourceEntity.SourceType sourceType);

    boolean existsByNoteBookIdAndNote_IdAndSourceType(UUID noteBookId, UUID noteId, NoteBookSourceEntity.SourceType sourceType);

    Optional<NoteBookSourceEntity> findByNoteBookIdAndId(UUID noteBookId, UUID sourceId);

    @Query("SELECT COUNT(ns) FROM NoteBookSourceEntity ns WHERE ns.noteBook.id = :noteBookId")
    long countByNoteBookId(UUID noteBookId);
}
