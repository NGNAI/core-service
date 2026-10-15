package ai.service.api;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.api.RagApiCore;
import ai.dto.outer.rag.request.DraftRagCreateRequestDto;
import ai.dto.outer.rag.request.DraftRagReviseRequestDto;
import ai.dto.outer.rag.request.NotebookRagCompletionRequestDto;
import ai.dto.outer.rag.request.RagCompletionRequestDto;
import ai.dto.outer.rag.request.RagSourceGuideRequestDto;
import ai.dto.outer.rag.request.TopicRagCompletionRequestDto;
import ai.dto.outer.rag.response.RagDraftDocumentTypeDto;
import ai.dto.outer.rag.response.RagDraftFormatStandardDto;
import ai.dto.outer.rag.response.RagSourceGuideResponseDto;
import ai.enums.ApiResponseStatus;
import ai.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class RagApiService {
    RagApiCore apiCore;

    ObjectMapper objectMapper;

    public Flux<String> topicChat(TopicRagCompletionRequestDto requestDto) throws JsonProcessingException {
        return apiCore.post("/v2/chat/completions", requestDto);
    }

    public Flux<String> noteBookChat(NotebookRagCompletionRequestDto requestDto) throws JsonProcessingException {
        return apiCore.post("/v2/notebook/chat/completions", requestDto);
    }

    public Flux<String> draftCreate(DraftRagCreateRequestDto requestDto) throws JsonProcessingException {
        return apiCore.post("/draft/create", requestDto);
    }

    public Flux<String> draftRevise(DraftRagReviseRequestDto requestDto) throws JsonProcessingException {
        return apiCore.post("/draft/revise", requestDto);
    }

    /**
     * Lấy metadata các loại tài liệu hỗ trợ cho draft (document types).
     * Quản lý tập trung ở RAG service (GET /draft/document-types).
     * @return danh sách document types
     */
    public List<RagDraftDocumentTypeDto> getDraftDocumentTypes() {
        return getListOrSingle("/draft/document-types", RagDraftDocumentTypeDto.class);
    }

    /**
     * Lấy metadata các chuẩn định dạng văn bản hỗ trợ cho draft (format standards).
     * Quản lý tập trung ở RAG service (GET /draft/format-standards).
     * @return danh sách format standards
     */
    public List<RagDraftFormatStandardDto> getDraftFormatStandards() {
        return getListOrSingle("/draft/format-standards", RagDraftFormatStandardDto.class);
    }

    /**
     * Gọi GET trả JSON metadata và chuẩn hóa về List, hỗ trợ cả response dạng array lẫn object đơn
     * (ví dụ format-standards hiện trả 1 object duy nhất).
     * @param endPoint path GET
     * @param elementType class của từng phần tử
     * @return danh sách phần tử
     */
    private <T> List<T> getListOrSingle(String endPoint, Class<T> elementType) {
        try {
            String raw = apiCore.getForObject(endPoint, String.class);
            if (raw == null || raw.isBlank()) {
                return List.of();
            }
            JsonNode node = objectMapper.readTree(raw);
            if (node.isArray()) {
                return objectMapper.convertValue(node,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, elementType));
            }
            return List.of(objectMapper.convertValue(node, elementType));
        } catch (WebClientResponseException e) {
            log.error("RAG GET {} failed with status {}:", endPoint, e.getStatusCode(), e);
            throw new AppException(ApiResponseStatus.RAG_SERVICE_UNAVAILABLE);
        } catch (JsonProcessingException e) {
            log.error("RAG GET {} response parse failed:", endPoint, e);
            throw new AppException(ApiResponseStatus.RAG_SERVICE_UNAVAILABLE);
        }
    }

    public String general(RagCompletionRequestDto requestDto) throws JsonProcessingException {
        return apiCore.postForString("/generate/v1/chat/completions_simple", requestDto);
    }

    /**
     * Completion không stream với timeout tùy chọn.
     *
     * <p>Dùng cho tác vụ cần giới hạn thời gian chờ riêng (ví dụ gợi ý câu hỏi khi
     * người dùng đang gõ) thay vì chờ read-timeout mặc định của RAG client.
     *
     * @param requestDto request completion (đã áp dụng AI settings)
     * @param timeout    thời gian chờ tối đa; {@code null} = dùng timeout mặc định
     * @return raw response body
     */
    public String general(RagCompletionRequestDto requestDto, Duration timeout) throws JsonProcessingException {
        return apiCore.postForString("/generate/v1/chat/completions_simple", requestDto, timeout);
    }

    /**
     * Trigger sinh source-guide (summary) cho một file trong notebook (NotebookLM).
     * RAG service sẽ callback kết quả về callback_url khi xử lý xong.
     * @param requestDto thông tin trigger (file_id, notebook_id, ..., callback_url)
     * @return response ban đầu (thường status = processing)
     */
    public RagSourceGuideResponseDto triggerSourceGuide(RagSourceGuideRequestDto requestDto) throws JsonProcessingException {
        return apiCore.postForObject("/notebook/v2/source-guide", requestDto, RagSourceGuideResponseDto.class);
    }

    /**
     * Lấy kết quả source-guide (summary) của một file trong notebook.
     * @param fileId ID của file/source
     * @param notebookId ID của notebook chứa file/source
     * @return response (status có thể là completed / failed / processing / not_found)
     */
    public RagSourceGuideResponseDto getSourceGuide(String fileId, String notebookId) {
        return apiCore.getForObject("/notebook/v2/source-guide?file_id={fileId}&notebook_id={notebookId}",
                RagSourceGuideResponseDto.class, fileId, notebookId);
    }

    /**
     * Xuất nội dung bản soạn thảo (draft) ra file Word (.docx) qua RAG service.
     *
     * <p>Đây là endpoint trả về <b>file nhị phân</b> (không phải JSON/SSE), nên phải dùng
     * {@link RagApiCore#getForBytes} thay vì {@link RagApiCore#getForObject}.
     *
     * <p>Query param được đưa vào URI template dạng {@code {name}} + truyền value qua uriVariables
     * (giống các method khác trong {@link RagApiCore}) để Spring vừa ghép {@code baseUrl} vừa encode
     * đúng giá trị — tên file có thể chứa tiếng Việt / ký tự đặc biệt như {@code &}, {@code ?}.
     *
     * @param sessionId      session soạn thảo trên RAG (draft.sessionId), bắt buộc
     * @param filename       tên file mong muốn; {@code null}/rỗng thì bỏ qua để RAG tự đặt tên
     * @param formatStandard chuẩn định dạng (format_standard); {@code null}/rỗng thì dùng mặc định của RAG
     * @return nội dung file .docx
     */
    public byte[] exportDraftDocx(String sessionId, String filename, String formatStandard) {
        StringBuilder endPoint = new StringBuilder("/draft/{sessionId}/export/docx");
        List<Object> uriVariables = new ArrayList<>();
        uriVariables.add(sessionId);

        boolean hasQueryParam = false;
        if (StringUtils.hasText(filename)) {
            endPoint.append("?filename={filename}");
            uriVariables.add(filename);
            hasQueryParam = true;
        }
        if (StringUtils.hasText(formatStandard)) {
            endPoint.append(hasQueryParam ? "&" : "?")
                    .append("format_standard={formatStandard}");
            uriVariables.add(formatStandard);
        }

        try {
            byte[] content = apiCore.getForBytes(endPoint.toString(), uriVariables.toArray());
            if (content == null || content.length == 0) {
                log.error("RAG export draft docx trả về nội dung rỗng (session_id={})", sessionId);
                throw new AppException(ApiResponseStatus.DRAFT_EXPORT_FAILED);
            }
            return content;
        } catch (WebClientRequestException e) {
            // Sai URL / RAG chưa lên / timeout kết nối
            log.error("Không kết nối được RAG service để export draft docx (session_id={})", sessionId, e);
            throw new AppException(ApiResponseStatus.RAG_SERVICE_UNAVAILABLE);
        } catch (WebClientResponseException e) {
            // RagApiCore đã log chi tiết response; ở đây chỉ map sang error code nghiệp vụ
            if (e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                // RAG không tìm thấy session soạn thảo tương ứng
                throw new AppException(ApiResponseStatus.DRAFT_EXPORT_NOT_AVAILABLE);
            }
            if (e.getStatusCode().is4xxClientError()) {
                throw new AppException(ApiResponseStatus.INVALID_REQUEST_INFORMATION);
            }
            throw new AppException(ApiResponseStatus.DRAFT_EXPORT_FAILED);
        }
    }
}
