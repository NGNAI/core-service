package ai.mapper;

import ai.dto.own.response.AuditLogResponseDto;
import ai.entity.postgres.AuditLogEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {
    AuditLogResponseDto entityToResponseDto(AuditLogEntity entity);

    List<AuditLogResponseDto> entitiesToResponseDtos(List<AuditLogEntity> entities);
}
