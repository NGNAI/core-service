package ai.controller.user;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.dto.own.request.AiSuggestionRequestDto;
import ai.model.ApiResponseModel;
import ai.service.AiSuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * API gợi ý câu hỏi (ask-autocomplete) cho ô chat.
 *
 * <p>Nằm trong {@code /user/**} nên được JWT + {@code MaintenanceModeFilter} bảo vệ
 * sẵn bởi filter chain {@code @Order(3)} — không cần chỉnh {@code SecurityConfig}.
 */
@Tag(name = "AI Suggestion", description = "Gợi ý câu hỏi cho ô chat (ask-autocomplete)")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/user/ai")
@RestController
public class AiSuggestionUserController {

    AiSuggestionService aiSuggestionService;

    @Operation(
            summary = "Gợi ý câu hỏi theo prefix",
            description = """
                    Sinh danh sách câu hỏi gợi ý dựa trên phần văn bản người dùng đang gõ.
                    Trả về mảng rỗng (HTTP 200) khi prefix quá ngắn, khi Ollama quá tải,
                    hoặc khi không sinh được gợi ý — FE không cần xử lý lỗi cho luồng này.
                    """)
    @PostMapping("/suggestions")
    ResponseEntity<ApiResponseModel<List<String>>> suggest(@Valid @RequestBody AiSuggestionRequestDto requestDto) {
        List<String> suggestions = aiSuggestionService.suggest(requestDto.getPrefix());
        return ResponseEntity.ok(
                ApiResponseModel.<List<String>>builder()
                        .message("Get suggestions successfully")
                        .data(suggestions)
                        .build());
    }
}
