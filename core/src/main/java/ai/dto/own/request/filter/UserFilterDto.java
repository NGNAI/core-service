package ai.dto.own.request.filter;

import ai.specification.UserEntitySpecification;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserFilterDto extends PageableFilterDto{
    String keyword;
    String source;

    public Predicate createSpec(Path<?> root, CriteriaBuilder criteriaBuilder){
        return criteriaBuilder.and(
                UserEntitySpecification.buildKeyword(root,criteriaBuilder,keyword),
                UserEntitySpecification.buildSource(root,criteriaBuilder,source));
    }
}
