package ai.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import ai.entity.postgres.AttachmentEntity;

@Repository
public interface AttachmentRepository extends JpaRepository<AttachmentEntity, UUID>, JpaSpecificationExecutor<AttachmentEntity> {
}
