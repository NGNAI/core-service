package ai.dto.own.request.filter;

import ai.annotation.EnumValue;
import ai.constant.InputValidateKey;
import ai.entity.postgres.NoteEntity;
import ai.enums.ApiResponseStatus;
import ai.enums.NoteSourceType;
import ai.exeption.AppException;
import ai.util.StringUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoteFilterDto extends PageableFilterDto {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "updatedAt", "title");

    @Schema(description = "Search by note title/content")
    String keyword;

    @Schema(description = "Filter by note source type", exampleClasses = NoteSourceType.class)
    @EnumValue(enumClass = NoteSourceType.class, message = InputValidateKey.NOTE_SOURCE_TYPE_INVALID)
    String sourceType;

    @Schema(description = "Filter by topic id")
    UUID topicId;

    @Schema(description = "Filter by notebook id")
    UUID noteBookId;

    public NoteFilterDto() {
        super();
        setSortBy("createdAt");
        setSortDir("DESC");
    }

    public Specification<NoteEntity> createSpec() {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                String normalizedKeyword = "%" + StringUtil.removeAccent(keyword).toLowerCase(Locale.ROOT) + "%";
                Predicate titlePredicate = criteriaBuilder.like(
                        criteriaBuilder.function("unaccent", String.class, criteriaBuilder.lower(root.get("title"))),
                        normalizedKeyword);
                Predicate contentPredicate = criteriaBuilder.like(
                        criteriaBuilder.function("unaccent", String.class, criteriaBuilder.lower(root.get("content"))),
                        normalizedKeyword);
                predicates.add(criteriaBuilder.or(titlePredicate, contentPredicate));
            }

            if (sourceType != null && !sourceType.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("sourceType"), NoteSourceType.valueOf(sourceType)));
            }

            if (topicId != null) {
                predicates.add(criteriaBuilder.equal(root.get("topicId"), topicId));
            }

            if (noteBookId != null) {
                predicates.add(criteriaBuilder.equal(root.get("noteBookId"), noteBookId));
            }

            return predicates.isEmpty()
                    ? criteriaBuilder.conjunction()
                    : criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Override
    public Pageable createPageable() {
        int resolvedPage = pageNumber == null || pageNumber < 0 ? 0 : pageNumber;
        int resolvedSize = pageSize == null || pageSize <= 0 ? 10 : pageSize;

        String normalizedSortDir = sortDir == null ? "ASC" : sortDir.trim().toUpperCase(Locale.ROOT);
        if (!"ASC".equals(normalizedSortDir) && !"DESC".equals(normalizedSortDir)) {
            throw new AppException(ApiResponseStatus.NOTE_SORT_DIR_INVALID);
        }

        String resolvedSortField = sortBy == null || sortBy.isBlank() ? "createdAt" : sortBy.trim();
        if (!ALLOWED_SORT_FIELDS.contains(resolvedSortField)) {
            throw new AppException(ApiResponseStatus.NOTE_SORT_BY_INVALID);
        }

        Sort.Direction direction = "DESC".equals(normalizedSortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        if ("createdAt".equals(resolvedSortField) || "updatedAt".equals(resolvedSortField)) {
            return PageRequest.of(resolvedPage, resolvedSize, Sort.by(direction, "audit." + resolvedSortField));
        }
        return PageRequest.of(resolvedPage, resolvedSize, Sort.by(direction, resolvedSortField));
    }
}
