package ai.mapper;

import ai.dto.own.request.TopicCreateRequestDto;
import ai.dto.own.response.TopicResponseDto;
import ai.entity.postgres.TopicEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TopicMapper extends GeneralMapper{
    TopicEntity createRequestDtoToEntity(TopicCreateRequestDto entity);

    @Mapping(target = "createdAt", expression = "java(createdAtFromAudit(entity.getAudit()))")
    @Mapping(target = "createdBy", expression = "java(createdByFromAudit(entity.getAudit()))")
    @Mapping(target = "updatedAt", expression = "java(updatedAtFromAudit(entity.getAudit()))")
    @Mapping(target = "updatedBy", expression = "java(updatedByFromAudit(entity.getAudit()))")
    @Mapping(target = "ownerId", source = "owner.id")
    TopicResponseDto entityToResponseDto(TopicEntity entity);
}
