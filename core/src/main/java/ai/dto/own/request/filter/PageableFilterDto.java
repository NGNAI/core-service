package ai.dto.own.request.filter;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Arrays;
import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PROTECTED)
public class PageableFilterDto {
    @Schema(description = "Page number, default is 0", example = "0")
    Integer pageNumber = 0;
    @Schema(description = "Page size, default is 10", example = "10")
    Integer pageSize = 10;
    @Schema(description = "Sort by field", example = "name")
    String sortBy;
    @Schema(description = "Sort direction, default is ASC", example = "ASC")
    String sortDir;

    public Pageable createPageable(){
//        if(List.of("createdBy","createdAt","updatedBy","updatedAt").contains(sortBy))
//            sortBy = String.format("audit.%s",sortBy);
        Pageable pageable;
        if(sortBy!=null)
            pageable = PageRequest.of(pageNumber, pageSize, sortDir!=null ? Sort.by(Sort.Direction.valueOf(sortDir.toUpperCase()),sortBy) : Sort.by(sortBy));
        else
            pageable = PageRequest.of(pageNumber, pageSize);

        return pageable;
    }
}
