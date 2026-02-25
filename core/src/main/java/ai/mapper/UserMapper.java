package ai.mapper;

import ai.dto.own.request.UserCreateRequestDto;
import ai.dto.own.request.UserUpdateRequestDto;
import ai.dto.own.response.UserResponseDto;
import ai.entity.postgres.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper extends GeneralMapper{
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "roles", ignore = true)
    UserEntity createRequestDtoToEntity(UserCreateRequestDto entity);

    @Mapping(target = "userId", source = "userId", qualifiedByName = "uuidToString")
    UserResponseDto entityToResponseDto(UserEntity entity);

    @Mapping(target = "roles", ignore = true)
    void updateEntity(@MappingTarget UserEntity entity, UserUpdateRequestDto requestDTO);
}
