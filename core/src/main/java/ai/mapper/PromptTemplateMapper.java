package ai.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import ai.dto.own.request.PromptTemplateCreateRequestDto;
import ai.dto.own.request.PromptTemplateUpdateRequestDto;
import ai.dto.own.response.PromptTemplateResponseDto;
import ai.entity.postgres.PromptTemplateEntity;

/**
 * Mapper cho PromptTemplateEntity.
 * Dùng {@code NullValuePropertyMappingStrategy.IGNORE} để hỗ trợ partial update
 * (field nào truyền null thì giữ nguyên giá trị hiện tại).
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PromptTemplateMapper extends GeneralMapper {

    PromptTemplateEntity createRequestDtoToEntity(PromptTemplateCreateRequestDto requestDto);

    @Mapping(target = "createdAt", expression = "java(createdAtFromAudit(entity.getAudit()))")
    @Mapping(target = "createdBy", expression = "java(createdByFromAudit(entity.getAudit()))")
    @Mapping(target = "updatedAt", expression = "java(updatedAtFromAudit(entity.getAudit()))")
    @Mapping(target = "updatedBy", expression = "java(updatedByFromAudit(entity.getAudit()))")
    @Mapping(target = "ownerId", source = "owner.id")
    PromptTemplateResponseDto entityToResponseDto(PromptTemplateEntity entity);

    /**
     * Cập nhật entity từ update DTO. Các field scope/owner/organization/audit/id không được phép đổi.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "scope", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "audit", ignore = true)
    void updateEntity(@MappingTarget PromptTemplateEntity entity, PromptTemplateUpdateRequestDto requestDto);
}
