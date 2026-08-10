package ai.dto.own.response;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import ai.enums.PromptScope;
import ai.enums.PromptType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * Response prompt template — kèm audit fields (createdAt/createdBy/updatedAt/updatedBy) qua {@link AuditResponseDto}.
 */
@JsonPropertyOrder({
        "id",
        "title",
        "content",
        "promptType",
        "scope",
        "displayOrder",
        "isActive",
        "ownerId"
})
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PromptTemplateResponseDto extends AuditResponseDto {
    UUID id;
    String title;
    String content;
    PromptType promptType;
    PromptScope scope;
    Integer displayOrder;
    Boolean isActive;

    /**
     * Owner của prompt (chỉ có với scope=USER). SYSTEM prompt trả về null.
     */
    UUID ownerId;
}
