package ai.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ai.dto.own.response.AttachmentResponseDto;
import ai.entity.postgres.AttachmentEntity;

@Mapper(componentModel = "spring")
public interface AttachmentMapper extends GeneralMapper {
    @Mapping(target = "createdAt", expression = "java(createdAtFromAudit(entity.getAudit()))")
    @Mapping(target = "createdBy", expression = "java(createdByFromAudit(entity.getAudit()))")
    @Mapping(target = "updatedAt", expression = "java(updatedAtFromAudit(entity.getAudit()))")
    @Mapping(target = "updatedBy", expression = "java(updatedByFromAudit(entity.getAudit()))")
    @Mapping(source = "owner.id", target = "ownerId")
    @Mapping(source = "organization.id", target = "orgId")
    AttachmentResponseDto entityToResponseDto(AttachmentEntity entity);
}
