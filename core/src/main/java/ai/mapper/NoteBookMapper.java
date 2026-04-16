package ai.mapper;

import ai.dto.own.request.NoteBookCreateRequestDto;
import ai.dto.own.response.NoteBookResponseDto;
import ai.entity.postgres.NoteBookEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NoteBookMapper extends GeneralMapper{
    NoteBookEntity createRequestDtoToEntity(NoteBookCreateRequestDto entity);

    @Mapping(target = "createdAt", expression = "java(createdAtFromAudit(entity.getAudit()))")
    @Mapping(target = "createdBy", expression = "java(createdByFromAudit(entity.getAudit()))")
    @Mapping(target = "updatedAt", expression = "java(updatedAtFromAudit(entity.getAudit()))")
    @Mapping(target = "updatedBy", expression = "java(updatedByFromAudit(entity.getAudit()))")
    @Mapping(target = "ownerId", source = "owner.id")
    NoteBookResponseDto entityToResponseDto(NoteBookEntity entity);
}
