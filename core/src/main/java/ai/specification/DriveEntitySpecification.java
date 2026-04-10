package ai.specification;

import java.util.UUID;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

public class DriveEntitySpecification {
    public static Predicate buildParent(Path<?> root, CriteriaBuilder criteriaBuilder, UUID parentId) {
        if (parentId == null) {
            return criteriaBuilder.isNull(root.get("parent"));
        }
        return criteriaBuilder.equal(root.get("parent").get("id"), parentId);
    }
}
