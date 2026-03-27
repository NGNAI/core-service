package ai.repository;

import ai.entity.postgres.MediaEntity;
import ai.enums.IngestionStatus;
import ai.enums.MediaDeleteStatus;
import ai.enums.MediaUploadTarget;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MediaRepository extends JpaRepository<MediaEntity, UUID>, JpaSpecificationExecutor<MediaEntity> {
    Optional<MediaEntity> findByJobId(UUID jobId);

    Iterable<MediaEntity> findByTargetAndIngestionStatus(MediaUploadTarget target, IngestionStatus ingestionStatus);

    Iterable<MediaEntity> findByDeleteStatus(MediaDeleteStatus deleteStatus);
}
