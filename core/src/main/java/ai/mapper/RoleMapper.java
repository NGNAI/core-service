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
    RoleEntity createRequestDtoToEntity(RoleCreateRequestDto entity);

    @Mapping(target = "permissions", ignore = true)
    RoleResponseDto entityToResponseDto(RoleEntity entity);

    void updateEntity(@MappingTarget RoleEntity entity, RoleUpdateRequestDto requestDto);
}
