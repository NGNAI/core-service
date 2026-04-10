package ai.dto.own.request.filter;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import ai.entity.postgres.DriveEntity;
import ai.enums.ApiResponseStatus;
import ai.exeption.AppException;
import ai.specification.DriveEntitySpecification;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DriveFilterDto extends PageableFilterDto {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "name",
            "contentType",
            "fileSize",
            "createdAt",
                "updatedAt"
    );

    private static final Map<String, String> SORT_FIELD_MAPPING = Map.of(
            "type", "contentType",
            "size", "fileSize",
            "createdAt", "createdAt",
            "updatedAt", "updatedAt"
    );

    @Schema(description = "Parent ID to filter drive items", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID parentId;

    public DriveFilterDto() {
        super();
        setSortBy("name");
        setSortDir("ASC");
    }

    public Specification<DriveEntity> createSpec() {
        return (root, query, criteriaBuilder) -> {
            Predicate parentPredicate = DriveEntitySpecification.buildParent(root, criteriaBuilder, parentId);
            return criteriaBuilder.and(parentPredicate);
        };
    }

    @Override
    public Pageable createPageable() {
        int resolvedPage = pageNumber == null || pageNumber < 0 ? 0 : pageNumber;
        int resolvedSize = pageSize == null || pageSize <= 0 ? 10 : pageSize;

        String normalizedSortDir = sortDir == null ? "ASC" : sortDir.trim().toUpperCase(Locale.ROOT);
        if (!"ASC".equals(normalizedSortDir) && !"DESC".equals(normalizedSortDir)) {
            throw new AppException(ApiResponseStatus.DRIVE_SORT_DIR_INVALID);
        }

        if (sortBy == null || sortBy.isBlank()) {
            return PageRequest.of(resolvedPage, resolvedSize);
        }

        String resolvedSortField = SORT_FIELD_MAPPING.getOrDefault(sortBy, sortBy);
        if (!ALLOWED_SORT_FIELDS.contains(resolvedSortField)) {
            throw new AppException(ApiResponseStatus.DRIVE_SORT_BY_INVALID);
        }

        Sort.Direction direction = "DESC".equals(normalizedSortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort.Direction folderDirection = Sort.Direction.ASC.equals(direction)
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return PageRequest.of(
                resolvedPage,
                resolvedSize,
                Sort.by(
                        new Sort.Order(folderDirection, "folder"),
                        new Sort.Order(direction, resolvedSortField)));
    }
}
