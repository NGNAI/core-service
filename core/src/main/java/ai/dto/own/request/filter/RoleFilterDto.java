package ai.dto.own.request.filter;

import ai.entity.postgres.RoleEntity;
import ai.util.StringUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleFilterDto extends PageableFilterDto{
    String keyword;

    public Specification<RoleEntity> createSpec(){
        return ((root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if(keyword!=null && !keyword.isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.function("unaccent", String.class, criteriaBuilder.lower(root.get("name"))),
                        "%" + StringUtil.removeAccent(keyword).toLowerCase() + "%"
                ));

                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.function("unaccent", String.class, criteriaBuilder.lower(root.get("description"))),
                        "%" + StringUtil.removeAccent(keyword).toLowerCase() + "%"
                ));
            }

            if (predicates.isEmpty())
                return criteriaBuilder.conjunction();

            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        });
    }
}
