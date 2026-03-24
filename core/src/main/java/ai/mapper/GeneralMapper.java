package ai.mapper;

import ai.entity.postgres.embeddable.AuditEmbed;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.time.Instant;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface GeneralMapper {
    default String createdAtFromAudit(AuditEmbed audit){
        return audit!=null ? audit.getCreatedAt().toString() : null;
    }

    default UUID createdByFromAudit(AuditEmbed audit){
        return audit!=null ? audit.getCreatedBy(): null;
    }

    default String updatedAtFromAudit(AuditEmbed audit){
        return audit!=null ? audit.getUpdatedAt().toString(): null;
    }

    default UUID updatedByFromAudit(AuditEmbed audit){
        return audit!=null ? audit.getUpdatedBy(): null;
    }

    @Named("instantToString")
    default String instantToString(Instant time) {
        return time!=null ? time.toString() : null;
    }
}
