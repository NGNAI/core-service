package ai.mapper;

import ai.dto.own.request.UserCreateRequestDto;
import ai.dto.own.request.UserProfileUpdateRequestDto;
import ai.dto.own.request.UserUpdateRequestDto;
import ai.dto.own.response.UserProfileResponseDto;
import ai.dto.own.response.UserResponseDto;
import ai.dto.own.response.UserWithOrgResponseDto;
import ai.dto.own.response.UserWithRoleInOrgResponseDto;
import ai.entity.postgres.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper extends GeneralMapper{
    @Mapping(target = "password", ignore = true)
    UserEntity createRequestDtoToEntity(UserCreateRequestDto entity);

    @Mapping(target = "createdAt", expression = "java(createdAtFromAudit(entity.getAudit()))")
    @Mapping(target = "createdBy", expression = "java(createdByFromAudit(entity.getAudit()))")
    @Mapping(target = "updatedAt", expression = "java(updatedAtFromAudit(entity.getAudit()))")
    @Mapping(target = "updatedBy", expression = "java(updatedByFromAudit(entity.getAudit()))")
    @Mapping(source = "lastLogin", target = "lastLogin", qualifiedByName = "instantToString")
    UserResponseDto entityToResponseDto(UserEntity entity);

    UserProfileResponseDto entityToProfileResponseDto(UserEntity entity);

    UserWithOrgResponseDto entityToWithOrgResponseDto(UserEntity entity);

    @Mapping(target = "createdAt", expression = "java(createdAtFromAudit(entity.getAudit()))")
    @Mapping(target = "createdBy", expression = "java(createdByFromAudit(entity.getAudit()))")
    @Mapping(target = "updatedAt", expression = "java(updatedAtFromAudit(entity.getAudit()))")
    @Mapping(target = "updatedBy", expression = "java(updatedByFromAudit(entity.getAudit()))")
    UserWithRoleInOrgResponseDto entityToWithRoleResponseDto(UserEntity entity);

    void updateEntity(@MappingTarget UserEntity entity, UserUpdateRequestDto requestDTO);
    void updateEntity(@MappingTarget UserEntity entity, UserProfileUpdateRequestDto requestDTO);
}
