package ai.mapper;

import ai.dto.own.response.FeedbackResponseDto;
import ai.entity.postgres.FeedbackEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FeedbackMapper extends GeneralMapper {
    @Mapping(target = "createdAt", expression = "java(createdAtFromAudit(entity.getAudit()))")
    @Mapping(target = "createdBy", expression = "java(createdByFromAudit(entity.getAudit()))")
    @Mapping(target = "updatedAt", expression = "java(updatedAtFromAudit(entity.getAudit()))")
    @Mapping(target = "updatedBy", expression = "java(updatedByFromAudit(entity.getAudit()))")
    @Mapping(source = "sender.id", target = "senderId")
    @Mapping(source = "sender.userName", target = "senderName")
    @Mapping(source = "senderOrg.id", target = "senderOrgId")
    @Mapping(source = "senderOrg.name", target = "senderOrgName")
    @Mapping(source = "responder.id", target = "responderId")
    @Mapping(source = "responder.userName", target = "responderName")
    @Mapping(source = "responderOrg.id", target = "responderOrgId")
    @Mapping(source = "responderOrg.name", target = "responderOrgName")
    FeedbackResponseDto entityToResponseDto(FeedbackEntity entity);
}
