package ai.dto.own.request;

import ai.constant.InputValidateKey;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FeedbackUpdateRequestDto {
    String subject;

    String content;

    /**
     * Dùng Boolean (wrapper) thay vì boolean để chỉ cập nhật isPrivate
     * khi client gửi giá trị, tránh reset về false khi update một phần.
     */
    Boolean isPrivate;
}
