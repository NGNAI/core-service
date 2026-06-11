package ai.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ai.dto.own.response.DraftResponseDto;
import ai.dto.own.response.DraftVersionResponseDto;
import ai.entity.postgres.DraftEntity;
import ai.entity.postgres.DraftVersionEntity;

@Mapper(componentModel = "spring")
public interface DraftMapper extends GeneralMapper {
    @Mapping(target = "createdAt", expression = "java(createdAtFromAudit(entity.getAudit()))")
    @Mapping(target = "createdBy", expression = "java(createdByFromAudit(entity.getAudit()))")
    @Mapping(target = "updatedAt", expression = "java(updatedAtFromAudit(entity.getAudit()))")
    @Mapping(target = "updatedBy", expression = "java(updatedByFromAudit(entity.getAudit()))")
    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "organizationId", source = "organization.id")
    DraftResponseDto entityToResponseDto(DraftEntity entity);
    
    @Mapping(target = "createdAt", expression = "java(createdAtFromAudit(entity.getAudit()))")
    @Mapping(target = "createdBy", expression = "java(createdByFromAudit(entity.getAudit()))")
    @Mapping(target = "updatedAt", expression = "java(updatedAtFromAudit(entity.getAudit()))")
    @Mapping(target = "updatedBy", expression = "java(updatedByFromAudit(entity.getAudit()))")
    @Mapping(target = "draftId", source = "draft.id")
    DraftVersionResponseDto versionEntityToResponseDto(DraftVersionEntity entity);
}