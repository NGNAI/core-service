package ai.dto.own.request.filter;

import ai.annotation.StringValue;
import ai.constant.InputValidateKey;
import ai.entity.postgres.OrganizationEntity;
import ai.util.StringUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PROTECTED)
public class OrganizationFilterDto extends PageableFilterDto{
    String keyword;

    public Specification<OrganizationEntity> createSpec(){
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

    @StringValue(acceptedValues = {"createdAt","updatedAt","name"}, ignoreCase = false, message = InputValidateKey.INVALID_SORT_FIELD_VALUE)
    @Override
    public String getSortBy(){
        return super.getSortBy();
    }
}
