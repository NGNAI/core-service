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
@Schema(description = "Unauthorized response (401)")
public class UnauthorizedResponseDto {
    @Builder.Default
    @Schema(example = "401", description = "HTTP status code")
    int status = 401;

    @Schema(example = "Authentication required")
    String message;
}
