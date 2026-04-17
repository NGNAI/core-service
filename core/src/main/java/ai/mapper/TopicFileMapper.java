package ai.mapper;

import ai.dto.own.response.TopicFileResponseDto;
import ai.entity.postgres.TopicFileEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {DataIngestionMapper.class})
public interface TopicFileMapper extends GeneralMapper {
    @Mapping(target = "createdAt", expression = "java(createdAtFromAudit(entity.getAudit()))")
    @Mapping(target = "createdBy", expression = "java(createdByFromAudit(entity.getAudit()))")
    @Mapping(target = "updatedAt", expression = "java(updatedAtFromAudit(entity.getAudit()))")
    @Mapping(target = "updatedBy", expression = "java(updatedByFromAudit(entity.getAudit()))")
    @Mapping(source = "topic.id", target = "topicId")
    TopicFileResponseDto entityToResponseDto(TopicFileEntity entity);
}
