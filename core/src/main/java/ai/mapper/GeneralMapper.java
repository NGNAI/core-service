package ai.mapper;

import ai.entity.postgres.embeddable.AuditEmbed;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GeneralMapper {
    default String createdDateFromAudit(AuditEmbed audit){
        return audit.toString();
    }

    default String createdByFromAudit(AuditEmbed audit){
        return audit.toString();
    }

    default String updatedDateFromAudit(AuditEmbed audit){
        return audit.toString();
    }

    default String updatedByFromAudit(AuditEmbed audit){
        return audit.toString();
    }
}
