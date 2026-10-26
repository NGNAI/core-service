package ai.service;

import java.time.Duration;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ai.AppProperties;
import ai.enums.ApiResponseStatus;
import ai.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

/**
 * Tổng hợp tài liệu OpenAPI của các service bên ngoài về chung Swagger UI của core-service.
 * <p>
 * Vì sao cần class này thay vì để {@code springdoc.swagger-ui.urls} trỏ thẳng ra ngoài:
 * <ul>
 *   <li>Trình duyệt của FE/khách hàng thường KHÔNG truy cập được IP nội bộ
 *       ({@code 192.168.30.73}) — chỉ core-service chạm được.</li>
 *   <li>Swagger UI chạy trên domain của core-service, fetch {@code /openapi.json} của
 *       service khác sẽ bị CORS chặn.</li>
 *   <li>Tài liệu của service nội bộ không nên lộ trực tiếp ra ngoài.</li>
 * </ul>
 * <p>
 * Cách hoạt động: gọi upstream lấy JSON OpenAPI, sau đó <b>ghi đè {@code servers}</b> để
 * nút "Try it out" của Swagger UI gọi qua chính route proxy của core-service
 * ({@code /api/v1/spellcheck/...}) thay vì gọi thẳng IP nội bộ.
 */
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class ExternalDocsService {

    /** Timeout chờ upstream trả file OpenAPI (ms). Tài liệu nhỏ, không cần chờ lâu. */
    static final Duration FETCH_TIMEOUT = Duration.ofSeconds(15);

    AppProperties appProperties;
    ObjectMapper objectMapper;

    /**
     * Xây dựng tài liệu OpenAPI của service Spellcheck, đã rewrite {@code servers}
     * để trỏ về route proxy của core-service.
     *
     * @param contextPathPathPrefix prefix path đầy đủ mà trình duyệt phải gọi để tới
     *                              route proxy (vd {@code /api/v1/spellcheck}).
     *                              Truyền {@code null} để KHÔNG rewrite servers.
     * @return chuỗi JSON OpenAPI đã xử lý
     */
    public String getSpellcheckDocs(String contextPathPathPrefix) {
        AppProperties.Spellcheck spellcheck = appProperties.getSpellcheck();
        if (spellcheck == null || spellcheck.getUrl() == null || spellcheck.getUrl().isBlank()) {
            throw new AppException(ApiResponseStatus.EXTERNAL_DOCS_NOT_CONFIGURED);
        }

        String openapiPath = (spellcheck.getOpenapiPath() == null || spellcheck.getOpenapiPath().isBlank())
                ? "/openapi.json"
                : spellcheck.getOpenapiPath();

        String rawJson;
        try {
            rawJson = WebClient.builder()
                    .baseUrl(spellcheck.getUrl())
                    .build()
                    .get()
                    .uri(openapiPath)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(FETCH_TIMEOUT);
        } catch (Exception e) {
            log.warn("Không lấy được OpenAPI của spellcheck từ {}{}: {}", spellcheck.getUrl(), openapiPath, e.getMessage());
            throw new AppException(ApiResponseStatus.EXTERNAL_DOCS_UNAVAILABLE);
        }

        if (rawJson == null || rawJson.isBlank()) {
            throw new AppException(ApiResponseStatus.EXTERNAL_DOCS_UNAVAILABLE);
        }

        try {
            JsonNode root = objectMapper.readTree(rawJson);
            if (contextPathPathPrefix != null && !contextPathPathPrefix.isBlank() && root instanceof ObjectNode objectNode) {
                rewriteServers(objectNode, contextPathPathPrefix);
            }
            // Trả String thay vì JsonNode: nếu controller trả JsonNode, Jackson sẽ
            // serialize nó như POJO (các cờ isArray/isObject/...) chứ không phải spec.
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            log.warn("OpenAPI json của spellcheck không parse được: {}", e.getMessage());
            throw new AppException(ApiResponseStatus.EXTERNAL_DOCS_UNAVAILABLE);
        }
    }

    /**
     * Ghi đè {@code servers} = [{url: <prefix>}] để mọi request "Try it out" đi qua
     * route proxy của core-service. Dùng URL tương đối → Swagger UI tự ghép với origin
     * đang mở, nên không cần biết domain thật của core-service.
     */
    private void rewriteServers(ObjectNode root, String prefix) {
        String normalized = prefix.startsWith("/") ? prefix : "/" + prefix;
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        var serverNode = objectMapper.createObjectNode();
        serverNode.put("url", normalized);
        serverNode.put("description", "Qua core-service (proxy, có xác thực JWT)");

        var servers = objectMapper.createArrayNode();
        servers.add(serverNode);
        root.set("servers", servers);

        // Ghi chú vào description để người đọc biết phải set Bearer token của core-service.
        String note = "\n\n> **Lưu ý:** Mọi request đi qua core-service và yêu cầu JWT của core-service "
                + "(bấm **Authorize** rồi dán token). Service gốc không có xác thực.";
        JsonNode info = root.get("info");
        if (info instanceof ObjectNode infoNode) {
            String desc = infoNode.hasNonNull("description") ? infoNode.get("description").asText() : "";
            infoNode.put("description", desc + note);
        }
    }
}
