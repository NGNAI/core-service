package ai.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ai.dto.own.response.DriveResponseDto;
import ai.entity.postgres.DriveEntity;

@Mapper(componentModel = "spring")
public interface DriveMapper extends GeneralMapper {
    @Mapping(target = "createdAt", expression = "java(createdAtFromAudit(entity.getAudit()))")
    @Mapping(target = "createdBy", expression = "java(createdByFromAudit(entity.getAudit()))")
    @Mapping(target = "updatedAt", expression = "java(updatedAtFromAudit(entity.getAudit()))")
    @Mapping(target = "updatedBy", expression = "java(updatedByFromAudit(entity.getAudit()))")
    @Mapping(source = "parent.id", target = "parentId")
    @Mapping(source = "organization.id", target = "orgId")
    @Mapping(source = "owner.id", target = "ownerId")
    DriveResponseDto entityToResponseDto(DriveEntity entity);
}
