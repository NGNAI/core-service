package ai.repository;

import ai.entity.postgres.DataIngestionEntity;
import ai.enums.IngestionStatus;
import ai.enums.DataIngestionDeleteStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DataIngestionRepository extends JpaRepository<DataIngestionEntity, UUID>, JpaSpecificationExecutor<DataIngestionEntity> {
    Optional<DataIngestionEntity> findByJobId(UUID jobId);

    Iterable<DataIngestionEntity> findByIngestionStatus(IngestionStatus ingestionStatus);

    Iterable<DataIngestionEntity> findByDeleteStatus(DataIngestionDeleteStatus deleteStatus);

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
}
