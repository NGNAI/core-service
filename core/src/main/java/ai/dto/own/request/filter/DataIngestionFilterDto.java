package ai.dto.own.request.filter;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import ai.entity.postgres.DataIngestionEntity;
import ai.enums.ApiResponseStatus;
import ai.exeption.AppException;
import ai.specification.DataIngestionEntitySpecification;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DataIngestionFilterDto extends PageableFilterDto {
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "name",
            "type",
            "size",
            "createdAt",
            "updatedAt",
            "downloadCount",
            "ingestionStatus",
            "accessLevel"
    );

    @Schema(description = "Organization ID to filter data ingestion", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID orgId;

    @Schema(description = "Owner ID to filter data ingestion", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID ownerId;

    @Schema(description = "Parent ID to filter data ingestion", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID parentId;

    public Specification<DataIngestionEntity> createSpec() {
        return (root, query, criteriaBuilder) -> {
            Predicate orgIdPredicate = DataIngestionEntitySpecification.buildOrgId(root, criteriaBuilder, orgId);
            Predicate ownerPredicate = DataIngestionEntitySpecification.buildOwnerId(root, criteriaBuilder, ownerId);
            Predicate parentPredicate = DataIngestionEntitySpecification.buildParent(root, criteriaBuilder, parentId);

            return criteriaBuilder.and(orgIdPredicate, ownerPredicate, parentPredicate);
        };
    }

    @Override
    public Pageable createPageable() {
        int resolvedPage = pageNumber == null || pageNumber < 0 ? 0 : pageNumber;
        int resolvedSize = pageSize == null || pageSize <= 0 ? 10 : pageSize;

        String normalizedSortDir = sortDir == null ? "ASC" : sortDir.trim().toUpperCase(Locale.ROOT);
        if (!"ASC".equals(normalizedSortDir) && !"DESC".equals(normalizedSortDir)) {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_SORT_DIR_INVALID);
        }

        if (sortBy == null || sortBy.isBlank()) {
            return PageRequest.of(resolvedPage, resolvedSize);
        }

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new AppException(ApiResponseStatus.DATA_INGESTION_SORT_BY_INVALID);
        }

        Sort.Direction direction = "DESC".equals(normalizedSortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(resolvedPage, resolvedSize, Sort.by(direction, mapSortField(sortBy)));
    }

    private String mapSortField(String sortBy) {
        if ("createdAt".equals(sortBy)) {
            return "createdAt";
        }
        if ("updatedAt".equals(sortBy)) {
            return "updatedAt";
        }
        return sortBy;
    }
}
