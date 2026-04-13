package ai.dto.own.request.filter;

import ai.annotation.StringValue;
import ai.constant.InputValidateKey;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import io.swagger.v3.oas.annotations.media.Schema;

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
    @StringValue(acceptedValues = {"asc","desc"}, ignoreCase = true, message = InputValidateKey.INVALID_SORT_DIR_VALUE)
    String sortDir;
    @JsonIgnore
    String sortPrefix = "";

    public Pageable createPageable(){
        Pageable pageable;
        if(sortBy!=null){
            boolean auditSort = sortBy.contains("createdAt") || sortBy.contains("createdBy") || sortBy.contains("updatedAt") || sortBy.contains("updatedBy");
            sortBy = String.format("%s%s.%s",sortPrefix,auditSort?".audit":"",sortBy);

            pageable = PageRequest.of(pageNumber, pageSize, sortDir!=null ? Sort.by(Sort.Direction.valueOf(sortDir.toUpperCase()),sortBy) : Sort.by(sortBy));
        } else
            pageable = PageRequest.of(pageNumber, pageSize);

        return pageable;
    }
}
