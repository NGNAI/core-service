package ai.specification;

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
   
}
