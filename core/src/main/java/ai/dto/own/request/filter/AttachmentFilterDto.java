package ai.dto.own.request.filter;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import ai.entity.postgres.AttachmentEntity;
import ai.enums.ApiResponseStatus;
import ai.exeption.AppException;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttachmentFilterDto extends PageableFilterDto {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "name",
            "fileSize",
            "contentType",
            "createdAt",
            "updatedAt");

    @Schema(description = "Filter by topic id")
    UUID topicId;

    @Schema(description = "Filter by message id")
    UUID messageId;

    public AttachmentFilterDto() {
        super();
        setSortBy("createdAt");
        setSortDir("DESC");
    }

    public Specification<AttachmentEntity> createSpec() {
        return (root, query, criteriaBuilder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (topicId != null) {
                predicates.add(criteriaBuilder.equal(root.get("topicId"), topicId));
            }
            if (messageId != null) {
                predicates.add(criteriaBuilder.equal(root.get("messageId"), messageId));
            }

            return predicates.isEmpty()
                    ? criteriaBuilder.conjunction()
                    : criteriaBuilder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    @Override
    public Pageable createPageable() {
        int resolvedPage = pageNumber == null || pageNumber < 0 ? 0 : pageNumber;
        int resolvedSize = pageSize == null || pageSize <= 0 ? 10 : pageSize;

        String normalizedSortDir = sortDir == null ? "ASC" : sortDir.trim().toUpperCase(Locale.ROOT);
        if (!"ASC".equals(normalizedSortDir) && !"DESC".equals(normalizedSortDir)) {
            throw new AppException(ApiResponseStatus.ATTACHMENT_SORT_DIR_INVALID);
        }

        String resolvedSortField = sortBy == null || sortBy.isBlank() ? "createdAt" : sortBy.trim();
        if (!ALLOWED_SORT_FIELDS.contains(resolvedSortField)) {
            throw new AppException(ApiResponseStatus.ATTACHMENT_SORT_BY_INVALID);
        }

        Sort.Direction direction = "DESC".equals(normalizedSortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        if ("createdAt".equals(resolvedSortField) || "updatedAt".equals(resolvedSortField)) {
            return PageRequest.of(resolvedPage, resolvedSize, Sort.by(direction, "audit." + resolvedSortField));
        }
        return PageRequest.of(resolvedPage, resolvedSize, Sort.by(direction, resolvedSortField));
    }
}
