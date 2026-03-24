package ai.repository;

import ai.entity.postgres.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<PermissionEntity, UUID>, JpaSpecificationExecutor<PermissionEntity> {
    boolean existsByName(String name);
}
