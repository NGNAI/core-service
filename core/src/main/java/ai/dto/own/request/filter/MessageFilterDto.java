package ai.dto.own.request.filter;

import ai.annotation.StringValue;
import ai.constant.InputValidateKey;
import ai.util.StringUtil;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MessageFilterDto extends PageableFilterDto{
    UUID lastMessageId;
    String keyword;
    List<String> types;
//    @NotBlank(message = InputValidateKey.MESSAGE_PARENT_ID_CAN_NOT_BE_NULL_OR_EMPTY)
//    UUID parentId;
//    @NotBlank(message = InputValidateKey.MESSAGE_PARENT_TYPE_CAN_NOT_BE_NULL_OR_EMPTY)
//    @EnumValue(enumClass = MessageParentType.class, message = InputValidateKey.INVALID_MESSAGE_PARENT_VALUE)
//    String parentType;

    public Predicate createSpec(Path<?> root, CriteriaBuilder criteriaBuilder){
        List<Predicate> predicates = new ArrayList<>();
        if(lastMessageId!=null) {
            predicates.add(criteriaBuilder.lessThan(root.get("id"), lastMessageId));
        }

        if(keyword!=null && !keyword.isEmpty()) {
            predicates.add(criteriaBuilder.like(
                    criteriaBuilder.function("unaccent", String.class, criteriaBuilder.lower(root.get("content"))),
                    "%" + StringUtil.removeAccent(keyword).toLowerCase() + "%"
            ));
        }

        if (types != null && !types.isEmpty()) {
            predicates.add(root.get("type").in(types));
        }

        if (predicates.isEmpty())
            return criteriaBuilder.conjunction();

        return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    }

    @StringValue(acceptedValues = {"id","createdAt","updatedAt","content"}, ignoreCase = false, message = InputValidateKey.INVALID_SORT_FIELD_VALUE)
    @Override
    public String getSortBy(){
        return super.getSortBy();
    }
}
