package ai.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ai.entity.postgres.DraftEntity;

@Repository
public interface DraftRepository extends JpaRepository<DraftEntity, UUID> {
    @Query("""
        SELECT COUNT(d) > 0
        FROM DraftEntity d
        WHERE d.id = :id
          AND d.owner.id = :ownerId
    """)
    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);

    Page<DraftEntity> findByOwner_IdAndOrganization_IdOrderByAudit_UpdatedAtDesc(
            UUID ownerId,
            UUID organizationId,
            Pageable pageable);
            
    @Query("SELECT COUNT(d) FROM DraftEntity d")
    long countAllDrafts();
    
    @Query("SELECT COUNT(d) FROM DraftEntity d WHERE d.organization.id = :orgId")
    long countAllDraftsByOrgId(UUID orgId);
    
    @Query("SELECT d.type, COUNT(d) FROM DraftEntity d GROUP BY d.type")
    java.util.List<java.lang.Object[]> countByType();
    
    @Query("SELECT d.type, COUNT(d) FROM DraftEntity d WHERE d.organization.id = :orgId GROUP BY d.type")
    java.util.List<java.lang.Object[]> countByTypeByOrgId(UUID orgId);

    // Count drafts by updated date (day precision)
    // Use native query because JPQL DATE() does not support Instant type
    @Query(value = "SELECT COUNT(d) FROM draft d WHERE DATE(d.updated_at) = :date", nativeQuery = true)
    long countDraftsByDate(java.time.LocalDate date);

    // Count drafts by updated date and organization
    @Query(value = "SELECT COUNT(d) FROM draft d WHERE DATE(d.updated_at) = :date AND d.organization_id = :orgId", nativeQuery = true)
    long countDraftsByDateAndOrgId(java.time.LocalDate date, UUID orgId);
    
    // @Query("SELECT d.presentationStyle, COUNT(d) FROM DraftEntity d GROUP BY d.presentationStyle")
    // java.util.List<java.lang.Object[]> countByPresentationStyle();

    // Count drafts by date range
    @Query("""
        SELECT COUNT(d)
        FROM DraftEntity d
        WHERE d.organization.id IN :orgIds
        AND d.audit.createdAt >= COALESCE(:from, d.audit.createdAt)
        AND d.audit.createdAt <= COALESCE(:to, d.audit.createdAt)
        """)
    long countByDateRange(@Param("orgIds") Collection<UUID> orgIds, @Param("from") Instant from, @Param("to") Instant to);
}
