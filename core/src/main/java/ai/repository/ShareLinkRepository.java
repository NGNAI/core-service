package ai.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ai.entity.postgres.ShareLinkEntity;
import ai.enums.ShareResource;

@Repository
public interface ShareLinkRepository extends JpaRepository<ShareLinkEntity, UUID>, JpaSpecificationExecutor<ShareLinkEntity> {

    /**
     * Tìm link theo token (dùng cho public access filter).
     */
    Optional<ShareLinkEntity> findByToken(String token);

    /**
     * Kiểm tra token đã tồn tại (dùng khi sinh token để regenerate nếu trùng).
     */
    boolean existsByToken(String token);

    /**
     * List link của owner, filter theo resourceType (nullable) và resourceId (nullable),
     * sắp xếp theo createdAt DESC.
     */
    @Query("""
        SELECT s
        FROM ShareLinkEntity s
        WHERE s.ownerId = :ownerId
          AND (:resourceType IS NULL OR s.resourceType = :resourceType)
          AND (:resourceId IS NULL OR s.resourceId = :resourceId)
        ORDER BY s.audit.createdAt DESC
        """)
    Page<ShareLinkEntity> findByOwnerWithFilter(
            @Param("ownerId") UUID ownerId,
            @Param("resourceType") ShareResource resourceType,
            @Param("resourceId") UUID resourceId,
            Pageable pageable);

    /**
     * Đếm số link active (chưa revoke) của owner.
     */
    long countByOwnerIdAndRevokedAtIsNull(UUID ownerId);

    /**
     * Tăng viewCount và cập nhật lastViewedAt nguyên tử (tránh race condition).
     * Dùng native UPDATE để không phải load entity.
     */
    @Modifying
    @Query(value = "UPDATE share_links SET view_count = view_count + 1, last_viewed_at = :now WHERE id = :id", nativeQuery = true)
    void incrementViewCount(@Param("id") UUID id, @Param("now") Instant now);
}