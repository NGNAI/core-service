package ai.service.api;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;

import ai.api.RagApiCore;
import ai.dto.outer.rag.request.RagCompletionRequestDto;
import ai.dto.outer.rag.request.RagDraftCreateRequestDto;
import ai.dto.outer.rag.request.RagDraftReviseRequestDto;
import ai.dto.outer.rag.request.RagSourceGuideRequestDto;
import ai.dto.outer.rag.response.RagSourceGuideResponseDto;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class RagApiService {
    RagApiCore apiCore;

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
