package ai.dto.own.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Forbidden response (403)")
public class ForbiddenResponseDto {
    @Builder.Default
    @Schema(example = "403", description = "HTTP status code")
    int status = 403;

    @Schema(example = "Insufficient permissions")
    String message;
}
