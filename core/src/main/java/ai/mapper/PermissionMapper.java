package ai.mapper;

import ai.dto.own.request.PermissionCreateRequestDto;
import ai.dto.own.request.PermissionUpdateRequestDto;
import ai.dto.own.response.PermissionResponseDto;
import ai.entity.postgres.PermissionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    PermissionEntity createRequestDtoToEntity(PermissionCreateRequestDto entity);

    PermissionResponseDto entityToResponseDto(PermissionEntity entity);

    void updateEntity(@MappingTarget PermissionEntity entity, PermissionUpdateRequestDto requestDto);
}
