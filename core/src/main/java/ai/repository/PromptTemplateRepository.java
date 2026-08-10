package ai.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ai.entity.postgres.PromptTemplateEntity;

@Repository
public interface PromptTemplateRepository extends JpaRepository<PromptTemplateEntity, UUID>, JpaSpecificationExecutor<PromptTemplateEntity> {

    /**
     * Kiểm tra prompt tồn tại và thuộc về user (dùng cho ownership check khi user sửa/xóa prompt USER scope).
     */
    @Query("""
        SELECT COUNT(p) > 0
        FROM PromptTemplateEntity p
        WHERE p.id = :id
          AND p.owner.id = :ownerId
    """)
    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);
}
