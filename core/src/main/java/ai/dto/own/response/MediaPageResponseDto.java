package ai.dto.own.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "MediaPageResponse", description = "Paged media response payload")
public class MediaPageResponseDto {
    @ArraySchema(schema = @Schema(implementation = MediaResponseDto.class), arraySchema = @Schema(description = "Current page items"))
    List<MediaResponseDto> items;

    @Schema(description = "Current page index (0-based)", example = "0")
    Integer pageNumber;

    @Schema(description = "Current page size", example = "10")
    Integer pageSize;

    @Schema(description = "Total number of pages", example = "5")
    Integer totalPages;

    @Schema(description = "Total number of records", example = "42")
    Long totalElements;
}
