package ai.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface GeneralMapper {
    @Named("uuidToString")
    default String uuidToString(UUID uuid){
        return uuid.toString();
    }
}
