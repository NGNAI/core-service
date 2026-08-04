package ai.specification;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

public class DataIngestionEntitySpecification {
    public static Predicate buildOrgId(Path<?> root, CriteriaBuilder criteriaBuilder, UUID orgId) {
        if (orgId == null) {
            return criteriaBuilder.conjunction();
        }
        return criteriaBuilder.equal(root.get("orgId"), orgId);
    }

    public static Predicate buildOwnerId(Path<?> root, CriteriaBuilder criteriaBuilder, UUID ownerId) {
        if (ownerId == null) {
            return criteriaBuilder.conjunction();
        }
        return criteriaBuilder.equal(root.get("ownerId"), ownerId);
    }

    public static Predicate buildParent(Path<?> root, CriteriaBuilder criteriaBuilder, UUID parentId) {
        if (parentId == null) {
            return criteriaBuilder.isNull(root.get("parent"));
        }
        return criteriaBuilder.equal(root.get("parent").get("id"), parentId);
    }

    public static Predicate buildAccessLevel(Path<?> root, CriteriaBuilder criteriaBuilder, Object accessLevel) {
        if (accessLevel == null) {
            return criteriaBuilder.conjunction();
        }
        return criteriaBuilder.equal(root.get("accessLevel"), accessLevel);
    }

    public static Predicate buildFromSource(Path<?> root, CriteriaBuilder criteriaBuilder, Object fromSource) {
        if (fromSource == null) {
            return criteriaBuilder.conjunction();
        }
        return criteriaBuilder.equal(root.get("fromSource"), fromSource);
    }

    public static Predicate buildFromSources(Path<?> root, CriteriaBuilder criteriaBuilder, List<?> fromSources) {
        if (fromSources == null || fromSources.isEmpty()) {
            return criteriaBuilder.conjunction();
        }
        return root.get("fromSource").in(fromSources);
    }

    /**
     * Tìm kiếm theo từ khóa: khớp tên file (name) HOẶC loại file (contentType),
     * không phân biệt hoa thường (LIKE %keyword%).
     */
    public static Predicate buildKeyword(Path<?> root, CriteriaBuilder criteriaBuilder, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return criteriaBuilder.conjunction();
        }
        String pattern = "%" + keyword.trim().toLowerCase() + "%";
        return criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("contentType")), pattern)
        );
    }

    /**
     * Lọc theo khoảng thời gian tạo (created_at) từ createdFrom đến createdTo (bao gồm cả hai đầu).
     * Nếu chỉ có một đầu thì lọc tương ứng greaterThanOrEqualTo / lessThanOrEqualTo.
     */
    public static Predicate buildCreatedBetween(Path<?> root, CriteriaBuilder criteriaBuilder, Instant createdFrom, Instant createdTo) {
        if (createdFrom == null && createdTo == null) {
            return criteriaBuilder.conjunction();
        }
        Path<Instant> createdAt = root.get("audit").get("createdAt");
        if (createdFrom != null && createdTo != null) {
            return criteriaBuilder.between(createdAt, createdFrom, createdTo);
        }
        if (createdFrom != null) {
            return criteriaBuilder.greaterThanOrEqualTo(createdAt, createdFrom);
        }
        return criteriaBuilder.lessThanOrEqualTo(createdAt, createdTo);
    }

    /**
     * Lọc theo trạng thái ingestion (enum IngestionStatus).
     */
    public static Predicate buildIngestionStatus(Path<?> root, CriteriaBuilder criteriaBuilder, Object ingestionStatus) {
        if (ingestionStatus == null) {
            return criteriaBuilder.conjunction();
        }
        return criteriaBuilder.equal(root.get("ingestionStatus"), ingestionStatus);
    }

}
