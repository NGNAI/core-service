package ai.api;

import java.time.Duration;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Component
public class RagApiCore {
    WebClient ragWebClient;

    ObjectMapper objectMapper;

    public Flux<String> post(String endPoint, Object body) throws JsonProcessingException {
        String jsonBody = objectMapper.writeValueAsString(body);
        log.info("RAG POST {} request body:\n{}", endPoint, prettyPrint(jsonBody));
        return ragWebClient.post()
                .uri(endPoint)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(jsonBody)
                .retrieve()
                .bodyToFlux(String.class)
                .doOnError(WebClientResponseException.class, e ->
                        log.error("RAG POST {} failed with status {}:\n{}",
                                endPoint, e.getStatusCode(), e.getResponseBodyAsString()));
    }

    public String postForString(String endPoint, Object body) throws JsonProcessingException {
        return postForString(endPoint, body, null);
    }

    /**
     * Gọi POST đồng bộ (blocking) với timeout tùy chọn.
     *
     * <p>Dùng cho các tác vụ cần giới hạn thời gian chờ riêng, ví dụ sinh gợi ý câu hỏi
     * (autocomplete) — không thể chờ tới read-timeout mặc định của {@code ragWebClient}
     * (hiện là 360s) vì người dùng đang gõ.
     *
     * @param endPoint  path bắt đầu bằng "/"
     * @param body      body request sẽ được serialize thành JSON
     * @param timeout   thời gian chờ tối đa; {@code null} hoặc &lt;= 0 = dùng timeout mặc định của client
     * @return raw response body
     */
    public String postForString(String endPoint, Object body, Duration timeout) throws JsonProcessingException {
        String jsonBody = objectMapper.writeValueAsString(body);
        log.info("RAG POST {} request body:\n{}", endPoint, prettyPrint(jsonBody));
        try {
            Mono<String> response = ragWebClient.post()
                    .uri(endPoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(jsonBody)
                    .retrieve()
                    .bodyToMono(String.class);

            return (timeout != null && !timeout.isZero() && !timeout.isNegative())
                    ? response.block(timeout)
                    : response.block();
        } catch (WebClientResponseException e) {
            log.error("RAG POST {} failed with status {}:\n{}",
                    endPoint, e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }

    /**
     * Gọi POST đồng bộ (blocking) và parse response về object theo responseType.
     * Dùng cho các endpoint trả về JSON đơn lẻ (không phải SSE), ví dụ trigger source-guide.
     * @param endPoint path bắt đầu bằng "/"
     * @param body đối tượng request sẽ được serialize thành JSON
     * @param responseType class của response DTO
     * @return object đã parse
     */
    public <T> T postForObject(String endPoint, Object body, Class<T> responseType) throws JsonProcessingException {
        String jsonBody = objectMapper.writeValueAsString(body);
        log.info("RAG POST {} request body:\n{}", endPoint, prettyPrint(jsonBody));
        try {
            return ragWebClient.post()
                    .uri(endPoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(jsonBody)
                    .retrieve()
                    .bodyToMono(responseType)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("RAG POST {} failed with status {}:\n{}",
                    endPoint, e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }

    /**
     * Gọi GET đồng bộ (blocking) và parse response về object theo responseType.
     * Dùng cho các endpoint trả về JSON đơn lẻ, ví dụ GET source-guide.
     * @param endPoint path bắt đầu bằng "/", có thể chứa URI template (vd ?file_id={fileId}&notebook_id={notebookId})
     * @param responseType class của response DTO
     * @param uriVariables các biến URI template nếu có
     * @return object đã parse
     */
    public <T> T getForObject(String endPoint, Class<T> responseType, Object... uriVariables) {
        try {
            return ragWebClient.get()
                    .uri(endPoint, uriVariables)
                    .retrieve()
                    .bodyToMono(responseType)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("RAG GET {} failed with status {}:\n{}",
                    endPoint, e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }

    /**
     * Gọi GET đồng bộ (blocking) và trả về nội dung nhị phân (binary) của response.
     *
     * <p>Dùng cho các endpoint trả về file (ví dụ export draft ra .docx) — không dùng
     * {@link #getForObject} vì Jackson không deserialize được nội dung nhị phân.
     *
     * <p>Tham số theo đúng style các method khác trong class: {@code endPoint} là URI template
     * tương đối (bắt đầu bằng "/") + {@code uriVariables}. Đây là overload {@code uri(String, Object...)}
     * nên Spring vẫn ghép {@code baseUrl} và encode giá trị.
     *
     * <p><b>Lưu ý:</b> tuyệt đối KHÔNG build {@code java.net.URI} tương đối rồi gọi {@code uri(URI)} —
     * overload đó dùng URI nguyên trạng, không resolve với baseUrl, khiến request rơi về
     * {@code localhost:80} (lỗi {@code Connection refused: /[0:0:0:0:0:0:0:1]:80}).
     *
     * @param endPoint     path bắt đầu bằng "/", có thể chứa placeholder dạng {name}
     * @param uriVariables giá trị cho các placeholder theo đúng thứ tự xuất hiện
     * @return mảng byte nội dung file
     */
    public byte[] getForBytes(String endPoint, Object... uriVariables) {
        try {
            return ragWebClient.get()
                    .uri(endPoint, uriVariables)
                    .accept(MediaType.ALL)
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("RAG GET {} failed with status {}:\n{}",
                    endPoint, e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        } catch (WebClientRequestException e) {
            // Không kết nối được tới RAG (sai URL, service chưa lên, timeout mạng...)
            log.error("RAG GET {} request failed (không kết nối được RAG service):", endPoint, e);
            throw e;
        }
    }

    private String prettyPrint(String json) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(objectMapper.readTree(json));
        } catch (JsonProcessingException e) {
            return json;
        }
    }
}
