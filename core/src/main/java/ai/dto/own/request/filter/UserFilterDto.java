package ai.dto.own.request.filter;

import ai.annotation.StringValue;
import ai.constant.InputValidateKey;
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

    @StringValue(acceptedValues = {"createdAt","updatedAt","firstName","lastName","gender","email","phoneNumber","lastLogin","source","active"}, ignoreCase = false, message = InputValidateKey.INVALID_SORT_FIELD_VALUE)
    @Override
    public String getSortBy(){
        return super.getSortBy();
    }
}
