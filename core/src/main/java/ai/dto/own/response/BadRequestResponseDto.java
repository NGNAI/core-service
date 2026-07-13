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
@Schema(description = "Bad Request response (400)")
public class BadRequestResponseDto {
    @Builder.Default
    @Schema(example = "400", description = "HTTP status code")
    int status = 400;

    @Schema(example = "Invalid request parameters")
    String message;
}
