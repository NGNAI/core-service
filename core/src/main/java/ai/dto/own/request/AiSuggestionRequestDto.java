package ai.dto.own.request;

import ai.constant.InputValidateKey;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * Request sinh gợi ý câu hỏi cho ô chat (ask-autocomplete).
 *
 * <p>{@code prefix} là phần văn bản người dùng đang gõ. FE nên debounce ~250-350ms
 * trước khi gọi để tránh gọi model theo từng ký tự.
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiSuggestionRequestDto {

    /** Phần văn bản người dùng đã nhập trong ô chat. */
    @NotBlank(message = InputValidateKey.AI_SUGGESTION_PREFIX_CAN_NOT_BE_NULL_OR_EMPTY)
    String prefix;
}
