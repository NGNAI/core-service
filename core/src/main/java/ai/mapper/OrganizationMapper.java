package ai.mapper;

import ai.dto.own.request.OrganizationCreateRequestDto;
import ai.dto.own.request.OrganizationUpdateRequestDto;
import ai.dto.own.response.OrganizationResponseDto;
import ai.dto.own.response.OrganizationWithUserRoleDto;
import ai.entity.postgres.OrganizationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface OrganizationMapper {
    OrganizationEntity createRequestDtoToEntity(OrganizationCreateRequestDto entity);

    @Mapping(target = "children", ignore = true)
    OrganizationResponseDto entityToResponseDto(OrganizationEntity entity);

    @Mapping(target = "children", ignore = true)
    OrganizationWithUserRoleDto entityToWithUserRoleResponseDto(OrganizationEntity entity);

    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
    void updateEntity(@MappingTarget OrganizationEntity entity, OrganizationUpdateRequestDto requestDto);
}
