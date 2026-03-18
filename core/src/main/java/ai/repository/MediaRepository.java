package ai.repository;

import ai.entity.postgres.MediaEntity;
import ai.enums.MediaUploadTarget;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MediaRepository extends JpaRepository<MediaEntity, UUID> {
    Optional<MediaEntity> findByJobId(UUID jobId);

    @Query("select m from MediaEntity m " +
            "where m.orgId = :orgId " +
            "and (:ownerId is null or m.ownerId = :ownerId) " +
            "and ((:parentId is null and m.parent is null) or m.parent.id = :parentId) " +
            "and (:target is null or m.target = :target)")
    Page<MediaEntity> findByOrgAndOptionalOwnerAndParent(
            @Param("orgId") UUID orgId,
            @Param("ownerId") UUID ownerId,
            @Param("parentId") UUID parentId,
            @Param("target") MediaUploadTarget target,
            Pageable pageable
    );
}
