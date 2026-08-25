package ai.dto.own.request;

import ai.constant.InputValidateKey;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FeedbackCreateRequestDto {
    @NotBlank(message = InputValidateKey.FEEDBACK_SUBJECT_CAN_NOT_BE_NULL_OR_EMPTY)
    String subject;

    @NotBlank(message = InputValidateKey.FEEDBACK_CONTENT_CAN_NOT_BE_NULL_OR_EMPTY)
    String content;

    /**
     * Dùng Boolean (wrapper) thay vì boolean primitive.
     * Field boolean tên isPrivate + Lombok + Jackson sẽ bị đổi property thành 'private' (bỏ tiền tố is),
     * khiến client gửi isPrivate không bind được. Wrapper Boolean sinh getIsPrivate()/setIsPrivate()
     * nên Jackson giữ đúng tên 'isPrivate'.
     */
    Boolean isPrivate = false;
}
