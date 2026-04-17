package ai.mapper;

import ai.dto.own.response.NoteBookFileResponseDto;
import ai.entity.postgres.NoteBookFileEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {DataIngestionMapper.class})
public interface NoteBookFileMapper extends GeneralMapper {
    @Mapping(target = "createdAt", expression = "java(createdAtFromAudit(entity.getAudit()))")
    @Mapping(target = "createdBy", expression = "java(createdByFromAudit(entity.getAudit()))")
    @Mapping(target = "updatedAt", expression = "java(updatedAtFromAudit(entity.getAudit()))")
    @Mapping(target = "updatedBy", expression = "java(updatedByFromAudit(entity.getAudit()))")
    @Mapping(source = "noteBook.id", target = "noteBookId")
    NoteBookFileResponseDto entityToResponseDto(NoteBookFileEntity entity);
}
