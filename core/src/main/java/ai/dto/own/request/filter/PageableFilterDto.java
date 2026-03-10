package ai.dto.own.request.filter;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Data
@FieldDefaults(level = AccessLevel.PROTECTED)
public class PageableFilterDto {
    Integer pageNumber = 0;
    Integer pageSize = 10;
    String sortBy;
    String sortDir;

    public Pageable createPageable(){
        Pageable pageable;
        if(sortBy!=null)
            pageable = PageRequest.of(pageNumber, pageSize, sortDir!=null ? Sort.by(Sort.Direction.valueOf(sortDir.toUpperCase()),sortBy) : Sort.by(sortBy));
        else
            pageable = PageRequest.of(pageNumber, pageSize);

        return pageable;
    }
}
