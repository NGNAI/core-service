package ai.service.api;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.api.RagApiCore;
import ai.dto.outer.rag.request.RagCompletionRequestDto;
import ai.dto.outer.rag.request.RagDraftCreateRequestDto;
import ai.dto.outer.rag.request.RagDraftReviseRequestDto;
import ai.dto.outer.rag.request.RagSourceGuideRequestDto;
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

    public Flux<String> topicChat(RagCompletionRequestDto requestDto) throws JsonProcessingException {
        return apiCore.post("/v2/chat/completions", requestDto);
    }

    public Flux<String> noteBookChat(RagCompletionRequestDto requestDto) throws JsonProcessingException {
        return apiCore.post("/v2/notebook/chat/completions", requestDto);
    }

    public Flux<String> draftCreate(RagDraftCreateRequestDto requestDto) throws JsonProcessingException {
        return apiCore.post("/draft/create", requestDto);
    }

    public Flux<String> draftRevise(RagDraftReviseRequestDto requestDto) throws JsonProcessingException {
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
}
