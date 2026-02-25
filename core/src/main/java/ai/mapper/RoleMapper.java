package ai.mapper;

import ai.dto.own.request.RoleCreateRequestDto;
import ai.dto.own.request.RoleUpdateRequestDto;
import ai.dto.own.response.RoleResponseDto;
import ai.entity.postgres.RoleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    @Mapping(target = "permissions", ignore = true)
    RoleEntity createRequestDtoToEntity(RoleCreateRequestDto entity);

    RoleResponseDto entityToResponseDto(RoleEntity entity);

    @Mapping(target = "permissions", ignore = true)
    void updateEntity(@MappingTarget RoleEntity entity, RoleUpdateRequestDto requestDto);
}
