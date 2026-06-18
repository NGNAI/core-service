package ai.dto.own.request.filter;

import ai.enums.AuditAction;
import ai.enums.AuditResource;
import ai.enums.AuditStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;

import ai.entity.postgres.AuditLogEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@FieldDefaults(level = AccessLevel.PROTECTED)
public class AuditLogFilterDto {
    @Schema(description = "Filter by user id", example = "00000000-0000-0000-0000-000000000000")
    UUID userId;

    @Schema(description = "Filter by organization id", example = "00000000-0000-0000-0000-000000000000")
    UUID orgId;

    @Schema(description = "Filter by action (e.g. CREATE, UPDATE, DELETE)")
    AuditAction action;

    @Schema(description = "Filter by resource type (e.g. USER, ORG, DRAFT)")
    AuditResource resource;

    @Schema(description = "Filter by status (SUCCESS or FAILED)")
    AuditStatus status;

    @Schema(description = "Filter from timestamp (ISO-8601, e.g. 2026-01-01T00:00:00Z)")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    Instant from;

    @Schema(description = "Filter to timestamp (ISO-8601, e.g. 2026-12-31T23:59:59Z)")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    Instant to;

    @Schema(description = "Keyword to search in resource name or description")
    String keyword;

    @Schema(description = "Page number, default 0", example = "0")
    Integer pageNumber = 0;

    @Schema(description = "Page size, default 20", example = "20")
    Integer pageSize = 20;

    @JsonIgnore
    public Specification<AuditLogEntity> createSpec() {
        return (root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (userId != null) {
                predicates.add(criteriaBuilder.equal(root.get("userId"), userId));
            }

            if (orgId != null) {
                predicates.add(criteriaBuilder.equal(root.get("orgId"), orgId));
            }

            if (action != null) {
                predicates.add(criteriaBuilder.equal(root.get("action"), action));
            }

            if (resource != null) {
                predicates.add(criteriaBuilder.equal(root.get("resource"), resource));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), from));
            }

            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), to));
            }

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("resourceName")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("userName")), pattern)
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    @JsonIgnore
    public Pageable createPageable() {
        int page = pageNumber == null || pageNumber < 0 ? 0 : pageNumber;
        int size = pageSize == null || pageSize <= 0 ? 20 : Math.min(pageSize, 200);
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
