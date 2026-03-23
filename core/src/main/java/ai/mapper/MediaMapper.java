package ai.mapper;

import ai.dto.own.response.MediaResponseDto;
import ai.dto.own.response.MediaUploadResponseDto;
import ai.entity.postgres.MediaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MediaMapper {
    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "createdAt", source = "audit.createdAt")
    @Mapping(target = "updatedAt", source = "audit.updatedAt")
    MediaResponseDto entityToResponseDto(MediaEntity entity);

    @Mapping(target = "mediaId", source = "id")
    MediaUploadResponseDto entityToUploadResponseDto(MediaEntity entity);
}
