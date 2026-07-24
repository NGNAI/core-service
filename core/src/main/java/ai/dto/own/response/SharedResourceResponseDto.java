package ai.dto.own.response;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import ai.enums.ShareResource;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * Metadata của tài nguyên được share (public viewer view).
 * Chỉ chứa thông tin công khai — không lộ ownerId, organizationId.
 */
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonPropertyOrder({
        "resourceType",
        "resourceId",
        "title",
        "description",
        "instruction",
        "ownerDisplayName",
        "createdAt",
        "messageCount",
        "sourceCount"
})
public class SharedResourceResponseDto {
    ShareResource resourceType;

    UUID resourceId;

    String title;

    @Schema(description = "Mô tả (chỉ Notebook)")
    String description;

    @Schema(description = "Instruction (chỉ Notebook)")
    String instruction;

    @Schema(description = "Tên hiển thị của owner (firstName + lastName) — không lộ userId")
    String ownerDisplayName;

    Instant createdAt;

    @Schema(description = "Tổng số message trong conversation")
    long messageCount;

    @Schema(description = "Tổng số source attached")
    long sourceCount;

    @Schema(description = "Link có yêu cầu password không (true = FE cần hiện input password)")
    boolean passwordRequired;
}