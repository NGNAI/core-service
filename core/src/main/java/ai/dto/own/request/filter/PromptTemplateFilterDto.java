package ai.dto.own.request.filter;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import ai.annotation.StringValue;
import ai.constant.InputValidateKey;
import ai.entity.postgres.PromptTemplateEntity;
import ai.enums.PromptScope;
import ai.enums.PromptType;
import ai.util.StringUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * Filter cho danh sách prompt template — dùng chung cho user flow (list prompt của mình + system)
 * và admin flow (list tất cả prompt trong org).
 */
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PromptTemplateFilterDto extends PageableFilterDto {

    @Schema(description = "Tìm theo keyword trong title hoặc content")
    String keyword;

    @Schema(description = "Lọc theo loại chatbot: TOPIC / NOTEBOOK / BOTH")
    PromptType promptType;

    @Schema(description = "Lọc theo phạm vi: SYSTEM / USER (chủ yếu cho admin)")
    PromptScope scope;

    @Schema(description = "Lọc theo trạng thái hiệu lực (chủ yếu cho admin)")
    Boolean isActive;

    public Specification<PromptTemplateEntity> createSpec() {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + StringUtil.removeAccent(keyword).toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(
                                criteriaBuilder.function("unaccent", String.class, criteriaBuilder.lower(root.get("title"))),
                                pattern),
                        criteriaBuilder.like(
                                criteriaBuilder.function("unaccent", String.class, criteriaBuilder.lower(root.get("content"))),
                                pattern)
                ));
            }

            if (promptType != null) {
                predicates.add(criteriaBuilder.equal(root.get("promptType"), promptType));
            }

            if (scope != null) {
                predicates.add(criteriaBuilder.equal(root.get("scope"), scope));
            }

            if (isActive != null) {
                predicates.add(criteriaBuilder.equal(root.get("isActive"), isActive));
            }

            if (predicates.isEmpty())
                return criteriaBuilder.conjunction();

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    @StringValue(acceptedValues = {"createdAt","updatedAt","title","displayOrder","promptType"}, ignoreCase = false, message = InputValidateKey.INVALID_SORT_FIELD_VALUE)
    @Override
    public String getSortBy() {
        return super.getSortBy();
    }
}
