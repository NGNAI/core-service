package ai.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ai.entity.postgres.NoteEntity;

@Repository
public interface NoteRepository extends JpaRepository<NoteEntity, UUID>, JpaSpecificationExecutor<NoteEntity> {
    boolean existsByIdAndOwnerId(UUID noteId, UUID ownerId);

    boolean existsByIdAndOrganizationId(UUID noteId, UUID organizationId);

    @Query("SELECT COUNT(n) FROM NoteEntity n")
    long countAllNotes();

    @Query("SELECT COUNT(n) FROM NoteEntity n WHERE n.organization.id IN :orgIds")
    long countAllNotesByOrgIds(@Param("orgIds") Collection<UUID> orgIds);

    @Query("""
        SELECT COUNT(n)
        FROM NoteEntity n
        WHERE n.organization.id IN :orgIds
        AND n.audit.createdAt >= COALESCE(:from, n.audit.createdAt)
        AND n.audit.createdAt <= COALESCE(:to, n.audit.createdAt)
        """)
    long countByDateRange(@Param("orgIds") Collection<UUID> orgIds, @Param("from") Instant from, @Param("to") Instant to);

    @Query("""
        SELECT n.sourceType, COUNT(n)
        FROM NoteEntity n
        WHERE n.organization.id IN :orgIds
        AND n.sourceType IS NOT NULL
        AND n.audit.createdAt >= COALESCE(:from, n.audit.createdAt)
        AND n.audit.createdAt <= COALESCE(:to, n.audit.createdAt)
        GROUP BY n.sourceType
        """)
    List<Object[]> countBySourceTypeAndDateRange(
            @Param("orgIds") Collection<UUID> orgIds,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("""
        SELECT n.sourceType, COUNT(n)
        FROM NoteEntity n
        WHERE n.organization.id IN :orgIds
        AND n.sourceType IS NOT NULL
        GROUP BY n.sourceType
        """)
    List<Object[]> countBySourceType(@Param("orgIds") Collection<UUID> orgIds);
}
