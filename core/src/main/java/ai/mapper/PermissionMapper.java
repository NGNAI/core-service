package ai.mapper;

import ai.dto.own.request.PermissionCreateRequestDto;
import ai.dto.own.request.PermissionUpdateRequestDto;
import ai.dto.own.response.PermissionResponseDto;
import ai.entity.postgres.PermissionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PermissionMapper extends GeneralMapper{
    PermissionEntity createRequestDtoToEntity(PermissionCreateRequestDto entity);

    @Mapping(target = "createdAt", expression = "java(createdAtFromAudit(entity.getAudit()))")
    @Mapping(target = "createdBy", expression = "java(createdByFromAudit(entity.getAudit()))")
    @Mapping(target = "updatedAt", expression = "java(updatedAtFromAudit(entity.getAudit()))")
    @Mapping(target = "updatedBy", expression = "java(updatedByFromAudit(entity.getAudit()))")
    PermissionResponseDto entityToResponseDto(PermissionEntity entity);

    void updateEntity(@MappingTarget PermissionEntity entity, PermissionUpdateRequestDto requestDto);
}
