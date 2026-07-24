package ai.mapper;

import ai.dto.own.response.ShareLinkResponseDto;
import ai.entity.postgres.ShareLinkEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ShareLinkMapper extends GeneralMapper {

    @Mapping(target = "passwordRequired", source = "entity", qualifiedByName = "passwordRequired")
    @Mapping(target = "active", source = "entity", qualifiedByName = "active")
    @Mapping(target = "resourceTitle", ignore = true)
    @Mapping(target = "url", ignore = true)
    @Mapping(target = "createdAt", source = "audit.createdAt")
    @Mapping(target = "createdBy", source = "audit.createdBy")
    ShareLinkResponseDto entityToResponseDto(ShareLinkEntity entity);

    @Named("passwordRequired")
    default boolean passwordRequired(ShareLinkEntity entity) {
        return entity != null && entity.isPasswordRequired();
    }

    @Named("active")
    default boolean active(ShareLinkEntity entity) {
        return entity != null && !entity.isRevoked() && !entity.isExpired();
    }
}