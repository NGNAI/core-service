package ai.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}
