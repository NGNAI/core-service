package ai.specification;

import ai.entity.postgres.UserEntity;
import ai.util.StringUtil;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;

public class UserEntitySpecification {
    public static Predicate buildKeyword(Path<?> root, CriteriaBuilder criteriaBuilder, String keyword){
        if(keyword == null || keyword.isBlank()) {
            return criteriaBuilder.conjunction();
        }
        List<Predicate> predicatesKeyword = new ArrayList<>();

        String keywordValue = "%" + StringUtil.removeAccent(keyword).toLowerCase() + "%";

        predicatesKeyword.add(criteriaBuilder.like(
                criteriaBuilder.function("unaccent", String.class, criteriaBuilder.lower(root.get("userName"))),
                keywordValue
        ));

        predicatesKeyword.add(criteriaBuilder.like(
                criteriaBuilder.function("unaccent", String.class, criteriaBuilder.lower(root.get("firstName"))),
                keywordValue
        ));

        predicatesKeyword.add(criteriaBuilder.like(
                criteriaBuilder.function("unaccent", String.class, criteriaBuilder.lower(root.get("lastName"))),
                keywordValue
        ));

        predicatesKeyword.add(criteriaBuilder.like(
                criteriaBuilder.function("unaccent", String.class, criteriaBuilder.lower(root.get("email"))),
                keywordValue
        ));

        predicatesKeyword.add(criteriaBuilder.like(
                criteriaBuilder.function("unaccent", String.class, criteriaBuilder.lower(root.get("phoneNumber"))),
                keywordValue
        ));

        return criteriaBuilder.or(predicatesKeyword.toArray(new Predicate[0]));
    }

    public static Predicate buildSource(Path<?> root, CriteriaBuilder criteriaBuilder, String source) {
        if (source == null || source.isBlank())
            return criteriaBuilder.conjunction();

        return criteriaBuilder.equal(root.get("source"), source);
    }
}
