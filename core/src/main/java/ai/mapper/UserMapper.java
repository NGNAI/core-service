package ai.mapper;

import ai.dto.own.request.UserCreateRequestDto;
import ai.dto.own.request.UserUpdateRequestDto;
import ai.dto.own.response.UserResponseDto;
import ai.dto.own.response.UserWithOrgResponseDto;
import ai.dto.own.response.UserWithRoleInOrgResponseDto;
import ai.entity.postgres.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "password", ignore = true)
    UserEntity createRequestDtoToEntity(UserCreateRequestDto entity);

    UserResponseDto entityToResponseDto(UserEntity entity);

    UserWithOrgResponseDto entityToWithOrgResponseDto(UserEntity entity);
    UserWithRoleInOrgResponseDto entityToWithRoleResponseDto(UserEntity entity);

    void updateEntity(@MappingTarget UserEntity entity, UserUpdateRequestDto requestDTO);
}
