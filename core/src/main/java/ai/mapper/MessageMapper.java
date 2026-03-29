package ai.mapper;

import ai.dto.own.request.MessageCreateRequestDto;
import ai.dto.own.request.MessageUpdateRequestDto;
import ai.dto.own.request.RoleUpdateRequestDto;
import ai.dto.own.response.MessageResponseDto;
import ai.entity.postgres.MessageEntity;
import ai.entity.postgres.RoleEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface MessageMapper extends GeneralMapper{
    MessageEntity createRequestDtoToEntity(MessageCreateRequestDto entity);

    @Mapping(target = "createdAt", expression = "java(createdAtFromAudit(entity.getAudit()))")
    @Mapping(target = "createdBy", expression = "java(createdByFromAudit(entity.getAudit()))")
    @Mapping(target = "updatedAt", expression = "java(updatedAtFromAudit(entity.getAudit()))")
    @Mapping(target = "updatedBy", expression = "java(updatedByFromAudit(entity.getAudit()))")
    MessageResponseDto entityToResponseDto(MessageEntity entity);

    void updateEntity(@MappingTarget MessageEntity entity, MessageUpdateRequestDto requestDto);
}
