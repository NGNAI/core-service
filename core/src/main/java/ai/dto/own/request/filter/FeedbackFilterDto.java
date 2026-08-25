package ai.dto.own.request.filter;

import ai.enums.FeedbackStatus;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FeedbackFilterDto extends PageableFilterDto {
    List<FeedbackStatus> statuses;
    Boolean isPrivate;
    String senderName;
    String senderOrgName;

    public Specification<ai.entity.postgres.FeedbackEntity> createSpec() {
        return ((root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Status filter
            if (statuses != null && !statuses.isEmpty()) {
                predicates.add(root.get("status").in(statuses));
            }

            // Private filter
            if (isPrivate != null) {
                predicates.add(criteriaBuilder.equal(root.get("isPrivate"), isPrivate));
            }

            // Sender name filter (like)
            if (senderName != null && !senderName.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("sender").get("userName")),
                        "%" + senderName.toLowerCase() + "%"
                ));
            }

            // Sender org name filter (like)
            if (senderOrgName != null && !senderOrgName.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("senderOrg").get("name")),
                        "%" + senderOrgName.toLowerCase() + "%"
                ));
            }

            if (predicates.isEmpty()) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        });
    }
}
