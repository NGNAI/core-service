package ai.mapper;

import ai.dto.own.request.SystemSettingCreateRequestDto;
import ai.dto.own.request.SystemSettingUpdateRequestDto;
import ai.dto.own.response.SystemSettingResponseDto;
import ai.entity.postgres.SystemSettingEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SystemSettingMapper {

    SystemSettingEntity createRequestDtoToEntity(SystemSettingCreateRequestDto requestDto);

    SystemSettingResponseDto entityToResponseDto(SystemSettingEntity entity);

    @Mapping(target = "key", ignore = true)
    void updateEntity(@MappingTarget SystemSettingEntity entity, SystemSettingUpdateRequestDto requestDto);
}
