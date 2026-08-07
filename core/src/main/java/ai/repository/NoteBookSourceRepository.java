package ai.repository;

import ai.entity.postgres.NoteBookSourceEntity;
import ai.enums.DataIngestionDeleteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NoteBookSourceRepository extends JpaRepository<NoteBookSourceEntity, UUID> {

    List<NoteBookSourceEntity> findByNoteBookId(UUID noteBookId);

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
                        (ns.jobId IS NULL AND ns.vectorStatus IN (ai.entity.postgres.NoteBookSourceEntity.VectorStatus.CREATED, ai.entity.postgres.NoteBookSourceEntity.VectorStatus.FAILED)
                            AND (ns.dispatchRetryCount IS NULL OR ns.dispatchRetryCount < :maxRetry))
                        OR
                        (ns.jobId IS NOT NULL AND ns.vectorStatus IN (
                            ai.entity.postgres.NoteBookSourceEntity.VectorStatus.CREATED,
                            ai.entity.postgres.NoteBookSourceEntity.VectorStatus.EXTRACTING,
                            ai.entity.postgres.NoteBookSourceEntity.VectorStatus.CHUNKING,
                            ai.entity.postgres.NoteBookSourceEntity.VectorStatus.EMBEDDING,
                            ai.entity.postgres.NoteBookSourceEntity.VectorStatus.STORING
                        ))
                    )
        """)
        List<NoteBookSourceEntity> findSourcesForIngestionMaintenance(@Param("maxRetry") int maxRetry);

    @Query("""
            SELECT ns FROM NoteBookSourceEntity ns
            WHERE ns.deleteStatus = ai.enums.DataIngestionDeleteStatus.ACTIVE
                AND ns.vectorStatus = ai.entity.postgres.NoteBookSourceEntity.VectorStatus.COMPLETED
                AND (ns.summary IS NULL OR ns.summary = '')
            """)
    List<NoteBookSourceEntity> findCompletedWithoutSummary(Pageable pageable);

    @Query("SELECT COUNT(ns) FROM NoteBookSourceEntity ns WHERE ns.noteBook.id = :noteBookId")
    long countByNoteBookId(UUID noteBookId);
}
