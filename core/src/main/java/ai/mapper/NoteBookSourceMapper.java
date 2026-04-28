package ai.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ai.dto.own.response.NoteBookSourceResponseDto;
import ai.entity.postgres.NoteBookSourceEntity;

@Mapper(componentModel = "spring")
public interface NoteBookSourceMapper extends GeneralMapper {
    @Mapping(target = "createdAt", expression = "java(createdAtFromAudit(entity.getAudit()))")
    @Mapping(target = "createdBy", expression = "java(createdByFromAudit(entity.getAudit()))")
    @Mapping(target = "updatedAt", expression = "java(updatedAtFromAudit(entity.getAudit()))")
    @Mapping(target = "updatedBy", expression = "java(updatedByFromAudit(entity.getAudit()))")
    @Mapping(source = "noteBook.id", target = "noteBookId")
    @Mapping(source = "note.id", target = "noteId")
    NoteBookSourceResponseDto entityToResponseDto(NoteBookSourceEntity entity);
}
