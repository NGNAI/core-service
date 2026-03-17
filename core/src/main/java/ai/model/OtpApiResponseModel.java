package ai.model;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OtpApiResponseModel<T> {
    int status;
    String message;
    Integer count;
    T data;

    public boolean isSuccess(){
        return status==200;
    }
}
