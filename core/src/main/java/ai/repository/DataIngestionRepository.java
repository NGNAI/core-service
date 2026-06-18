package ai.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
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
    
    @Query("SELECT d.fromSource, COUNT(d) FROM DataIngestionEntity d GROUP BY d.fromSource")
    java.util.List<java.lang.Object[]> countBySource();
    
    @Query("SELECT d.fromSource, COUNT(d) FROM DataIngestionEntity d WHERE d.organization.id = :orgId GROUP BY d.fromSource")
    java.util.List<java.lang.Object[]> countBySourceByOrgId(UUID orgId);
    
    @Query("SELECT d.accessLevel, COUNT(d) FROM DataIngestionEntity d GROUP BY d.accessLevel")
    java.util.List<java.lang.Object[]> countByAccessLevel();
    
    @Query("SELECT d.accessLevel, COUNT(d) FROM DataIngestionEntity d WHERE d.organization.id = :orgId GROUP BY d.accessLevel")
    java.util.List<java.lang.Object[]> countByAccessLevelByOrgId(UUID orgId);
}
