package ai.mapper;

import ai.entity.postgres.embeddable.AuditEmbed;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GeneralMapper {
    default String createdAtFromAudit(AuditEmbed audit){
        return audit!=null ? audit.getCreatedAt().toString() : null;
    }

    default Integer createdByFromAudit(AuditEmbed audit){
        return audit!=null ? audit.getCreatedBy(): null;
    }

    default String updatedAtFromAudit(AuditEmbed audit){
        return audit!=null ? audit.getUpdatedAt().toString(): null;
    }

    default Integer updatedByFromAudit(AuditEmbed audit){
        return audit!=null ? audit.getUpdatedBy(): null;
    }
}
