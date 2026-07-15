package ai.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ai.entity.postgres.DataIngestionEntity;
import ai.enums.DataIngestionDeleteStatus;
import ai.enums.IngestionStatus;

@Repository
public interface DataIngestionRepository extends JpaRepository<DataIngestionEntity, UUID>, JpaSpecificationExecutor<DataIngestionEntity> {
    Optional<DataIngestionEntity> findByJobId(UUID jobId);

    Iterable<DataIngestionEntity> findByIngestionStatus(IngestionStatus ingestionStatus);

    Iterable<DataIngestionEntity> findByDeleteStatus(DataIngestionDeleteStatus deleteStatus);

    Iterable<DataIngestionEntity> findByDeleteStatusIn(Collection<DataIngestionDeleteStatus> deleteStatuses);

    @Query("SELECT d FROM DataIngestionEntity d WHERE d.ingestionStatus IS NOT NULL AND d.ingestionStatus NOT IN (ai.enums.IngestionStatus.COMPLETED, ai.enums.IngestionStatus.FAILED)")
    Iterable<DataIngestionEntity> findByIngestionStatusNotFinal();

        Optional<DataIngestionEntity> findFirstByFolderTrueAndNameAndParentIsNullAndOwnerIdAndOrganizationIdAndDeleteStatus(
            String name,
            UUID ownerId,
            UUID organizationId,
            DataIngestionDeleteStatus deleteStatus);

        Optional<DataIngestionEntity> findFirstByFolderTrueAndNameAndParentIdAndOwnerIdAndOrganizationIdAndDeleteStatus(
            String name,
            UUID parentId,
            UUID ownerId,
            UUID organizationId,
            DataIngestionDeleteStatus deleteStatus);
            
    @Query("SELECT COUNT(d) FROM DataIngestionEntity d")
    long countAllDataIngestions();
    
    @Query("SELECT COUNT(d) FROM DataIngestionEntity d WHERE d.organization.id = :orgId")
    long countAllDataIngestionsByOrgId(UUID orgId);
    
    @Query("SELECT d.ingestionStatus, COUNT(d) FROM DataIngestionEntity d GROUP BY d.ingestionStatus")
    java.util.List<java.lang.Object[]> countByStatus();
    
    @Query("SELECT d.ingestionStatus, COUNT(d) FROM DataIngestionEntity d WHERE d.organization.id = :orgId GROUP BY d.ingestionStatus")
    java.util.List<java.lang.Object[]> countByStatusByOrgId(UUID orgId);
    
    @Query("SELECT d.ingestionStatus, COUNT(d) FROM DataIngestionEntity d WHERE d.organization.id IN :orgIds GROUP BY d.ingestionStatus")
    java.util.List<java.lang.Object[]> countByStatusByOrgIds(@Param("orgIds") Collection<UUID> orgIds);

    @Query("""
        SELECT d.ingestionStatus, COUNT(d)
        FROM DataIngestionEntity d
        WHERE d.organization.id IN :orgIds
        AND d.audit.createdAt >= COALESCE(:from, d.audit.createdAt)
        AND d.audit.createdAt <= COALESCE(:to, d.audit.createdAt)
        GROUP BY d.ingestionStatus
        """)
    java.util.List<java.lang.Object[]> countByStatusByOrgIdsAndDateRange(
            @Param("orgIds") Collection<UUID> orgIds,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("SELECT d.fromSource, COUNT(d) FROM DataIngestionEntity d GROUP BY d.fromSource")
    java.util.List<java.lang.Object[]> countBySource();

    @Query("SELECT d.fromSource, COUNT(d) FROM DataIngestionEntity d WHERE d.organization.id = :orgId GROUP BY d.fromSource")
    java.util.List<java.lang.Object[]> countBySourceByOrgId(UUID orgId);

    @Query("SELECT d.fromSource, COUNT(d) FROM DataIngestionEntity d WHERE d.organization.id IN :orgIds GROUP BY d.fromSource")
    java.util.List<java.lang.Object[]> countBySourceByOrgIds(@Param("orgIds") Collection<UUID> orgIds);

    @Query("""
        SELECT d.fromSource, COUNT(d)
        FROM DataIngestionEntity d
        WHERE d.organization.id IN :orgIds
        AND d.audit.createdAt >= COALESCE(:from, d.audit.createdAt)
        AND d.audit.createdAt <= COALESCE(:to, d.audit.createdAt)
        GROUP BY d.fromSource
        """)
    java.util.List<java.lang.Object[]> countBySourceByOrgIdsAndDateRange(
            @Param("orgIds") Collection<UUID> orgIds,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("SELECT d.accessLevel, COUNT(d) FROM DataIngestionEntity d GROUP BY d.accessLevel")
    java.util.List<java.lang.Object[]> countByAccessLevel();

    @Query("SELECT d.accessLevel, COUNT(d) FROM DataIngestionEntity d WHERE d.organization.id = :orgId GROUP BY d.accessLevel")
    java.util.List<java.lang.Object[]> countByAccessLevelByOrgId(UUID orgId);

    @Query("SELECT d.accessLevel, COUNT(d) FROM DataIngestionEntity d WHERE d.organization.id IN :orgIds GROUP BY d.accessLevel")
    java.util.List<java.lang.Object[]> countByAccessLevelByOrgIds(@Param("orgIds") Collection<UUID> orgIds);

    @Query("""
        SELECT d.accessLevel, COUNT(d)
        FROM DataIngestionEntity d
        WHERE d.organization.id IN :orgIds
        AND d.audit.createdAt >= COALESCE(:from, d.audit.createdAt)
        AND d.audit.createdAt <= COALESCE(:to, d.audit.createdAt)
        GROUP BY d.accessLevel
        """)
    java.util.List<java.lang.Object[]> countByAccessLevelByOrgIdsAndDateRange(
            @Param("orgIds") Collection<UUID> orgIds,
            @Param("from") Instant from,
            @Param("to") Instant to);

    // Count data ingestions by updated date (day precision)
    @Query(value = "SELECT COUNT(d) FROM data_ingestion d WHERE DATE(d.updated_at) = :date", nativeQuery = true)
    long countDataIngestionsByDate(java.time.LocalDate date);

    // Count data ingestions by updated date and organization
    @Query(value = "SELECT COUNT(d) FROM data_ingestion d WHERE DATE(d.updated_at) = :date AND d.org_id = :orgId", nativeQuery = true)
    long countDataIngestionsByDateAndOrgId(java.time.LocalDate date, UUID orgId);

    // Count by owner group by owner (top N)
    @Query("""
        SELECT d.owner.id as userId, d.owner.userName as userName, COUNT(d) as cnt, COALESCE(SUM(d.fileSize), 0) as totalSize
        FROM DataIngestionEntity d
        WHERE d.organization.id IN :orgIds
        AND d.folder = false
        AND d.audit.createdAt >= COALESCE(:from, d.audit.createdAt)
        AND d.audit.createdAt <= COALESCE(:to, d.audit.createdAt)
        GROUP BY d.owner.id, d.owner.userName
        ORDER BY cnt DESC
        """)
    List<Object[]> countByOwnerGroupByOwnerAndDateRange(
            @Param("orgIds") Collection<UUID> orgIds,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    // Count by owner group by owner (top N) - all time
    @Query("""
        SELECT d.owner.id as userId, d.owner.userName as userName, COUNT(d) as cnt, COALESCE(SUM(d.fileSize), 0) as totalSize
        FROM DataIngestionEntity d
        WHERE d.organization.id IN :orgIds
        AND d.folder = false
        GROUP BY d.owner.id, d.owner.userName
        ORDER BY cnt DESC
        """)
    List<Object[]> countByOwnerGroupByOwner(@Param("orgIds") Collection<UUID> orgIds, Pageable pageable);

    // Sum file size by org and date range
    @Query("""
        SELECT COALESCE(SUM(d.fileSize), 0)
        FROM DataIngestionEntity d
        WHERE d.organization.id IN :orgIds
        AND d.folder = false
        AND d.audit.createdAt >= COALESCE(:from, d.audit.createdAt)
        AND d.audit.createdAt <= COALESCE(:to, d.audit.createdAt)
        """)
    long sumFileSizeByOrgIdsAndDateRange(
            @Param("orgIds") Collection<UUID> orgIds,
            @Param("from") Instant from,
            @Param("to") Instant to);

    // Sum file size by org
    @Query("SELECT COALESCE(SUM(d.fileSize), 0) FROM DataIngestionEntity d WHERE d.organization.id IN :orgIds AND d.folder = false")
    long sumFileSizeByOrgIds(@Param("orgIds") Collection<UUID> orgIds);

    // Count by content type and date range
    @Query("""
        SELECT d.contentType, COUNT(d)
        FROM DataIngestionEntity d
        WHERE d.organization.id IN :orgIds
        AND d.contentType IS NOT NULL
        AND d.folder = false
        AND d.audit.createdAt >= COALESCE(:from, d.audit.createdAt)
        AND d.audit.createdAt <= COALESCE(:to, d.audit.createdAt)
        GROUP BY d.contentType
        ORDER BY COUNT(d) DESC
        """)
    List<Object[]> countByContentTypeAndDateRange(
            @Param("orgIds") Collection<UUID> orgIds,
            @Param("from") Instant from,
            @Param("to") Instant to);

    // Count by content type
    @Query("""
        SELECT d.contentType, COUNT(d)
        FROM DataIngestionEntity d
        WHERE d.organization.id IN :orgIds
        AND d.contentType IS NOT NULL
        AND d.folder = false
        GROUP BY d.contentType
        ORDER BY COUNT(d) DESC
        """)
    List<Object[]> countByContentType(@Param("orgIds") Collection<UUID> orgIds);

    // Count by date range
    @Query("""
        SELECT COUNT(d)
        FROM DataIngestionEntity d
        WHERE d.organization.id IN :orgIds
        AND d.audit.createdAt >= COALESCE(:from, d.audit.createdAt)
        AND d.audit.createdAt <= COALESCE(:to, d.audit.createdAt)
        """)
    long countByDateRange(@Param("orgIds") Collection<UUID> orgIds, @Param("from") Instant from, @Param("to") Instant to);
}
