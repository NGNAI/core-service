package ai.mapper;

import ai.dto.own.request.RoleCreateRequestDto;
import ai.dto.own.request.RoleUpdateRequestDto;
import ai.dto.own.response.RoleResponseDto;
import ai.dto.own.response.RoleSimplifyResponseDto;
import ai.entity.postgres.RoleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface RoleMapper extends GeneralMapper{
    RoleEntity createRequestDtoToEntity(RoleCreateRequestDto entity);

    @Mapping(target = "createdAt", expression = "java(createdAtFromAudit(entity.getAudit()))")
    @Mapping(target = "createdBy", expression = "java(createdByFromAudit(entity.getAudit()))")
    @Mapping(target = "updatedAt", expression = "java(updatedAtFromAudit(entity.getAudit()))")
    @Mapping(target = "updatedBy", expression = "java(updatedByFromAudit(entity.getAudit()))")
    @Mapping(target = "permissions", ignore = true)
    RoleResponseDto entityToResponseDto(RoleEntity entity);

    @Mapping(target = "permissions", ignore = true)
    RoleSimplifyResponseDto entityToSimplifyResponseDto(RoleEntity entity);

    void updateEntity(@MappingTarget RoleEntity entity, RoleUpdateRequestDto requestDto);
}
