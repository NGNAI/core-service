package ai.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import ai.dto.own.request.MessageCreateRequestDto;
import ai.dto.own.request.MessageUpdateRequestDto;
import ai.dto.own.response.MessageResponseDto;
import ai.entity.postgres.MessageEntity;

@Mapper(componentModel = "spring")
public interface MessageMapper extends GeneralMapper{
    MessageEntity createRequestDtoToEntity(MessageCreateRequestDto entity);

    @Mapping(target = "createdAt", expression = "java(createdAtFromAudit(entity.getAudit()))")
    @Mapping(target = "createdBy", expression = "java(createdByFromAudit(entity.getAudit()))")
    @Mapping(target = "updatedAt", expression = "java(updatedAtFromAudit(entity.getAudit()))")
    @Mapping(target = "updatedBy", expression = "java(updatedByFromAudit(entity.getAudit()))")
    @Mapping(target = "suggestedReplies", expression = "java(suggestedRepliesOrDefault(entity.getSuggestedReplies()))")
    @Mapping(target = "reasoningSteps", expression = "java(reasoningStepsOrDefault(entity.getReasoningSteps()))")
    MessageResponseDto entityToResponseDto(MessageEntity entity);

    void updateEntity(@MappingTarget MessageEntity entity, MessageUpdateRequestDto requestDto);
}
