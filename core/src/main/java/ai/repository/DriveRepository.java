package ai.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import ai.entity.postgres.DriveEntity;
import ai.enums.DataIngestionDeleteStatus;

@Repository
public interface DriveRepository extends JpaRepository<DriveEntity, UUID>, JpaSpecificationExecutor<DriveEntity> {
    List<DriveEntity> findByOrganizationIdAndOwnerIdAndDeleteStatus(
            UUID organizationId,
            UUID ownerId,
            DataIngestionDeleteStatus deleteStatus);

    List<DriveEntity> findByParentIdAndOwnerIdAndOrganizationIdAndDeleteStatus(
            UUID parentId,
            UUID ownerId,
            UUID organizationId,
            DataIngestionDeleteStatus deleteStatus);

    Optional<DriveEntity> findFirstByFolderTrueAndNameAndParentIsNullAndOwnerIdAndOrganizationIdAndDeleteStatus(
            String name,
            UUID ownerId,
            UUID organizationId,
            DataIngestionDeleteStatus deleteStatus);

    Optional<DriveEntity> findFirstByFolderTrueAndNameAndParentIdAndOwnerIdAndOrganizationIdAndDeleteStatus(
            String name,
            UUID parentId,
            UUID ownerId,
            UUID organizationId,
            DataIngestionDeleteStatus deleteStatus);
}
