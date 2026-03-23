package ai.dto.own.request.filter;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import ai.entity.postgres.MediaEntity;
import ai.enums.ApiResponseStatus;
import ai.enums.MediaUploadTarget;
import ai.exeption.AppException;
import ai.specification.MediaEntitySpecification;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MediaFilterDto extends PageableFilterDto {
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

    @Schema(description = "Organization ID to filter media", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID orgId;

    @Schema(description = "Owner ID to filter media", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID ownerId;

    @Schema(description = "Parent ID to filter media", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID parentId;

    @Schema(description = "Target to filter media", example = "UPLOAD")
    MediaUploadTarget target;

    public Specification<MediaEntity> createSpec() {
        return (root, query, criteriaBuilder) -> {
            Predicate orgIdPredicate = MediaEntitySpecification.buildOrgId(root, criteriaBuilder, orgId);
            Predicate ownerPredicate = MediaEntitySpecification.buildOwnerId(root, criteriaBuilder, ownerId);
            Predicate parentPredicate = MediaEntitySpecification.buildParent(root, criteriaBuilder, parentId);
            Predicate targetPredicate = MediaEntitySpecification.buildTarget(root, criteriaBuilder, target);

            return criteriaBuilder.and(orgIdPredicate, ownerPredicate, parentPredicate, targetPredicate);
        };
    }

    @Override
    public Pageable createPageable() {
        int resolvedPage = pageNumber == null || pageNumber < 0 ? 0 : pageNumber;
        int resolvedSize = pageSize == null || pageSize <= 0 ? 10 : pageSize;

        String normalizedSortDir = sortDir == null ? "ASC" : sortDir.trim().toUpperCase(Locale.ROOT);
        if (!"ASC".equals(normalizedSortDir) && !"DESC".equals(normalizedSortDir)) {
            throw new AppException(ApiResponseStatus.MEDIA_SORT_DIR_INVALID);
        }

        if (sortBy == null || sortBy.isBlank()) {
            return PageRequest.of(resolvedPage, resolvedSize);
        }

        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new AppException(ApiResponseStatus.MEDIA_SORT_BY_INVALID);
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
