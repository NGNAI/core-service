package ai.repository;

import ai.entity.postgres.NoteBookSourceEntity;
import ai.enums.DataIngestionDeleteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NoteBookSourceRepository extends JpaRepository<NoteBookSourceEntity, UUID> {

    Page<NoteBookSourceEntity> findByNoteBookId(UUID noteBookId, Pageable pageable);

    boolean existsByNoteBookIdAndDisplayNameAndSourceType(UUID noteBookId, String displayName, NoteBookSourceEntity.SourceType sourceType);

    boolean existsByNoteBookIdAndNote_IdAndSourceType(UUID noteBookId, UUID noteId, NoteBookSourceEntity.SourceType sourceType);

    Optional<NoteBookSourceEntity> findByNoteBookIdAndId(UUID noteBookId, UUID sourceId);

        Optional<NoteBookSourceEntity> findByJobId(UUID jobId);

        Iterable<NoteBookSourceEntity> findByDeleteStatusIn(Collection<DataIngestionDeleteStatus> deleteStatuses);

        @Query("""
                SELECT ns FROM NoteBookSourceEntity ns
                WHERE ns.deleteStatus = ai.enums.DataIngestionDeleteStatus.ACTIVE
                    AND (
                        (ns.jobId IS NULL AND ns.vectorStatus IN (ai.entity.postgres.NoteBookSourceEntity.VectorStatus.NOT_PROCESSED, ai.entity.postgres.NoteBookSourceEntity.VectorStatus.ERROR))
                        OR
                        (ns.jobId IS NOT NULL AND ns.vectorStatus IN (ai.entity.postgres.NoteBookSourceEntity.VectorStatus.NOT_PROCESSED, ai.entity.postgres.NoteBookSourceEntity.VectorStatus.PROCESSING))
                    )
        """)
        List<NoteBookSourceEntity> findSourcesForIngestionMaintenance();

    @Query("SELECT COUNT(ns) FROM NoteBookSourceEntity ns WHERE ns.noteBook.id = :noteBookId")
    long countByNoteBookId(UUID noteBookId);
}
