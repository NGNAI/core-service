package ai.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.service.ExternalDocsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Endpoint <b>public</b> để Swagger UI của core-service lấy được tài liệu OpenAPI
 * của các service nội bộ (vd Spellcheck/RAG service) mà trình duyệt không cần
 * truy cập trực tiếp IP nội bộ.
 * <p>
 * Vì sao public: Swagger UI fetch file này bằng {@code fetch()} không kèm header
 * {@code Authorization}, nên nếu bảo vệ bằng JWT thì UI sẽ không tải được spec.
 * Dữ liệu trả về chỉ là mô tả API (không phải dữ liệu nghiệp vụ) — và bản thân
 * route gọi thật ({@code /spellcheck/**}) vẫn bắt buộc JWT.
 */
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/public/docs")
@Tag(name = "External API Docs", description = "Aggregated OpenAPI docs of internal services (public, read-only)")
@RestController
public class ExternalDocsPublicController {

    /**
     * Prefix proxy của spellcheck, tính từ gốc core-service.
     * Phải khớp với {@code spring.cloud.gateway.server.webmvc.routes[].predicates Path}
     * và {@code server.servlet.context-path} ({@code /api/v1}) — Swagger UI tự ghép
     * origin + giá trị này, nên cần đầy đủ {@code /api/v1}.
     */
    static final String SPELLCHECK_PROXY_PREFIX = "/api/v1/spellcheck";

    ExternalDocsService externalDocsService;

    @Operation(
            summary = "Spellcheck service OpenAPI",
            description = "Trả file OpenAPI của service Spellcheck (RAG service) và ghi đè `servers` "
                    + "để nút Try it out gọi qua route proxy của core-service."
    )
    @GetMapping(value = "/spellcheck", produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    ResponseEntity<String> getSpellcheckDocs() {
        return ResponseEntity.ok(externalDocsService.getSpellcheckDocs(SPELLCHECK_PROXY_PREFIX));
    }
}
