package ai.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.AppProperties;
import ai.constant.AiPromptTemplates;
import ai.dto.outer.rag.request.DraftRagCreateRequestDto;
import ai.dto.outer.rag.request.DraftRagReviseRequestDto;
import ai.dto.outer.rag.request.NotebookRagCompletionRequestDto;
import ai.dto.outer.rag.request.RagCompletionRequestDto;
import ai.dto.outer.rag.request.TopicRagCompletionRequestDto;
import ai.dto.outer.rag.response.RagDraftDocumentTypeDto;
import ai.dto.outer.rag.response.RagDraftFormatStandardDto;
import ai.dto.own.request.DraftChatRequestDto;
import ai.dto.own.request.DraftSaveVersionRequestDto;
import ai.dto.own.request.MessageCreateRequestDto;
import ai.dto.own.request.MessageUpdateRequestDto;
import ai.dto.own.request.NoteBookCreateConversationRequestDto;
import ai.dto.own.request.NoteBookCreateRequestDto;
import ai.dto.own.request.TopicCreateConversationRequestDto;
import ai.dto.own.request.TopicCreateRequestDto;
import ai.dto.own.request.filter.MessageFilterDto;
import ai.dto.own.response.DraftResponseDto;
import ai.dto.own.response.DraftVersionResponseDto;
import ai.dto.own.response.MessageResponseDto;
import ai.dto.own.response.TopicSourceResponseDto;
import ai.entity.postgres.DraftEntity;
import ai.entity.postgres.NoteBookEntity;
import ai.entity.postgres.TopicEntity;
import ai.enums.DataScope;
import ai.enums.MessageParentType;
import ai.enums.MessageType;
import ai.enums.SystemEventSource;
import ai.enums.SystemEventType;
import ai.enums.TopicType;
import ai.service.api.RagApiService;
import ai.util.AiTextSanitizer;
import ai.util.JwtUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class RagService {
    static final int DEFAULT_TOPIC_RECENT_MESSAGE_WINDOW = 10;
    static final int DEFAULT_NOTEBOOK_RECENT_MESSAGE_WINDOW = 14;
    static final int DEFAULT_MIN_MESSAGES_TO_COMPRESS = 4;

    /** Ngân sách ký tự tối đa của input đưa vào prompt sinh tiêu đề. */
    static final int DEFAULT_TITLE_MAX_INPUT_CHARS = 2000;
    /** Ngân sách ký tự tối đa của block hội thoại đưa vào prompt nén summary. */
    static final int DEFAULT_SUMMARY_MAX_INPUT_CHARS = 12000;
    /** Ngân sách từ tối đa của summary đầu ra (chặn summary phình to). */
    static final int DEFAULT_SUMMARY_MAX_WORDS = 300;
    /** Giới hạn ký tự cho từng tin nhắn khi ghép block hội thoại. */
    static final int DEFAULT_MESSAGE_MAX_CHARS = 1500;
    /** Temperature tất định cho tác vụ phụ trợ (sinh tiêu đề, nén summary). */
    static final double DETERMINISTIC_TEMPERATURE = 0.2;

    /** Số ký tự xấp xỉ cho 1 từ khi quy đổi ngân sách từ → ký tự (clamp output). */
    static final int APPROX_CHARS_PER_WORD = 7;
    /** Số từ tối đa của tiêu đề (clamp cứng, prompt đã yêu cầu 4–10 từ). */
    static final int TITLE_MAX_WORDS = 15;

    /** Giới hạn vòng reasoning khi tạo draft (theo text_drafting_guide, default 8, min 1, max 20) */
    static final int DRAFT_CREATE_MAX_ITERATIONS = 8;
    /** Giới hạn vòng reasoning khi revise draft (theo text_drafting_guide, default 5, min 1, max 15) */
    static final int DRAFT_REVISE_MAX_ITERATIONS = 5;

    AppProperties appProperties;
    RagApiService ragApiService;
    TopicService topicService;
    NoteBookService noteBookService;
    DraftService draftService;
    MessageService messageService;
    SystemEventSseService systemEventSseService;
    SystemSettingService systemSettingService;

    ObjectMapper objectMapper;

    /**
     * Generate a string response from RAG API based on the provided request DTO.
     * This method handles the JSON response, extracting the content from the
     * "choices" array and returning it as a string. If the response is empty or
     * does not contain the expected structure, it returns null.
     * @param requestDto
     * @return
     */
    public String generateString(RagCompletionRequestDto requestDto) {
        try {
            String response = ragApiService.general(requestDto);

            if (response == null || response.isBlank()) {
                return null;
            }

            JsonNode root = objectMapper.readTree(response);
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");

            if (contentNode.isTextual()) {
                return contentNode.asText();
            }

            if (contentNode.isArray()) {
                StringBuilder content = new StringBuilder();
                for (JsonNode part : contentNode) {
                    if (part.isTextual()) {
                        content.append(part.asText());
                    } else if (part.isObject()) {
                        JsonNode textPart = part.path("text");
                        if (textPart.isTextual()) {
                            content.append(textPart.asText());
                        }
                    }
                }
                return content.length() > 0 ? content.toString() : null;
            }

            return null;
        } catch (JsonProcessingException e) {
            log.error("Error processing JSON response from RAG API", e);
            return null;
        }
    }

    /**
     * Chat with topic, if topicId is null, create new topic and chat, else chat
     * with exist topic
     * 
     * @param topicId
     * @param requestDto
     * @return
     * @throws JsonProcessingException
     */
    public Flux<String> chatTopic(UUID topicId, TopicCreateConversationRequestDto requestDto,
            List<TopicSourceResponseDto> uploadedSources)
            throws JsonProcessingException {
        UUID capturedUserId = JwtUtil.getUserId();
        UUID capturedOrgId = JwtUtil.getOrgId();

        // If topic not exists, create new topic
        boolean isNewTopic = topicId == null;
        if (isNewTopic)
            topicId = topicService.create(TopicCreateRequestDto.builder()
                    .title(requestDto.getMessage())
                    .type(TopicType.PRIVATE.getValue())
                    .build()).getId();
        else
            topicService.validateTopicOfUser(topicId, capturedUserId);

        UUID finalTopicId = topicId;

        // Async: generate a better title via AI and notify FE via SSE
        if (isNewTopic) {
            asyncUpdateTopicTitle(finalTopicId, capturedOrgId, capturedUserId, requestDto.getMessage());
        }

        topicService.validateTopicId(finalTopicId);
        TopicEntity topicEntity = topicService.getEntityById(finalTopicId);

        MessageFilterDto messageFilterDto = new MessageFilterDto();
        messageFilterDto.setTypes(Arrays.asList(MessageType.USER.getValue(), MessageType.ASSISTANT.getValue()));
        messageFilterDto.setPageNumber(0);
        messageFilterDto.setPageSize(topicRecentMessageWindow());
        messageFilterDto.setSortBy("createdAt");
        messageFilterDto.setSortDir("desc");

        // Query history
        List<RagCompletionRequestDto.Message> historyConversations = messageService
                .getAll(finalTopicId, MessageParentType.TOPIC, messageFilterDto).getSecond()
                .stream()
                .map(messageResponseDto -> createRagMessage(messageResponseDto.getType(),
                        messageResponseDto.getContent()))
                .collect(Collectors.toList());

        Collections.reverse(historyConversations);

        historyConversations.add(createRagMessage(MessageType.USER.getValue(), requestDto.getMessage()));

        // Insert user question
        messageService.create(
                finalTopicId,
                MessageParentType.TOPIC,
                MessageCreateRequestDto.builder()
                        .content(requestDto.getMessage())
                        .type(MessageType.USER.getValue())
                        .build());

        // Insert assistant question
        MessageResponseDto assistantMessage = messageService.create(
                finalTopicId,
                MessageParentType.TOPIC,
                MessageCreateRequestDto.builder()
                        .content("Answering.....")
                        .type(MessageType.ASSISTANT.getValue())
                        .build());

        // Get attachments of topic - Khoa xử lý tiếp nha
        // List<TopicSourceResponseDto> attachments =
        // topicSourceService.getAllSources(finalTopicId);

        TopicRagCompletionRequestDto.Metadata metadata = new TopicRagCompletionRequestDto.Metadata();
        metadata.setUserId(JwtUtil.getUserId());
        metadata.setOrganizationId(JwtUtil.getOrgId());
        metadata.setTopicId(finalTopicId);
        metadata.setScopes(requestDto.getScopes());
        // metadata.setFileIds(attachments.stream().map(e ->
        // e.getId().toString()).collect(Collectors.toSet()));
        // Chỗ fileIds này tạm thời là lấy theo attachment của message đầu vào, sau này
        // có thể điều chỉnh lại nếu muốn lấy attachment theo topic thay vì message
        // (hiện tại FE chưa support upload attachment riêng cho message, mà chỉ có
        // upload attachment chung cho topic, nên tạm thời cứ lấy attachment của message
        // đầu vào đã, sau này nếu FE support upload attachment riêng cho message thì sẽ
        // lấy attachment theo message thay vì topic)
        metadata.setFileIds(uploadedSources != null
                ? uploadedSources.stream().map(e -> e.getId().toString()).collect(Collectors.toSet())
                : Collections.emptySet());
        metadata.setSummaries(buildSummaryMetadata(topicEntity));

        TopicRagCompletionRequestDto ragCompletionRequestDto = applyAiSettings(TopicRagCompletionRequestDto.builder()
                .messages(historyConversations)
                .metadata(metadata)
                .stream(true))
                .build();

        StringBuilder reasoningSteps = new StringBuilder();
        StringBuilder fullAnswer = new StringBuilder();
        StringBuilder source = new StringBuilder();
        StringBuilder suggestedReplies = new StringBuilder();

        return ragApiService.topicChat(ragCompletionRequestDto)
                .startWith(String.format("{\"messageId\": \"%s\"}", assistantMessage.getId()))
                .startWith(String.format("{\"topicId\": \"%s\"}", topicId))
                // Trả thêm về dto assistant message luôn
                .startWith(String.format("{\"assistantMessage\": %s}",
                        objectMapper.writeValueAsString(assistantMessage)))
                .doOnNext(raw -> {
                    try {
                        JsonNode node = objectMapper.readTree(raw);
                        // Bỏ qua các event metadata không có trường event (assistantMessage, topicId, messageId)
                        if (!node.has("event")) {
                            return;
                        }

                        switch (node.get("event").asText()) {
                            case "delta" -> {
                                if (node.has("content")) {
                                    fullAnswer.append(node.get("content").asText());
                                }
                            }
                            case "final_answer" -> {
                                if (node.has("reasoning_steps")) {
                                    reasoningSteps.setLength(0);
                                    reasoningSteps.append(node.get("reasoning_steps").toString());
                                }

                                if (node.has("sources")) {
                                    source.setLength(0);
                                    source.append(node.get("sources").toString());
                                }

                                if (node.has("content")) {
                                    fullAnswer.setLength(0);
                                    fullAnswer.append(node.get("content").asText());
                                }
                            }
                            case "sources" -> {
                                if (node.has("sources")) {
                                    source.setLength(0);
                                    source.append(node.get("sources").toString());
                                }
                            }
                            case "suggested_replies" -> {
                                JsonNode repliesNode = node.has("suggested_replies")
                                        ? node.get("suggested_replies")
                                        : node.get("content");
                                if (repliesNode != null && !repliesNode.isMissingNode()) {
                                    suggestedReplies.setLength(0);
                                    suggestedReplies.append(normalizeJsonArrayText(repliesNode));
                                }
                            }
                            // Dự phòng trường fullAnswer bị rỗng, thì sẽ lấy content từ event "done" (nếu có) để update lại fullAnswer
                            case "done" -> {
                                if (node.has("content") && fullAnswer.isEmpty()) {
                                    fullAnswer.setLength(0);
                                    fullAnswer.append(node.get("content").asText());
                                }
                            }
                            default -> {
                                // "done" và các event khác không cần xử lý
                            }
                        }
                    } catch (JsonProcessingException e) {
                        log.error("Fail to parse stream token", e);
                    }
                })
                .doOnComplete(() -> {
                    if (source.isEmpty()) {
                        source.append("[]");
                    }
                    if (suggestedReplies.isEmpty()) {
                        suggestedReplies.append("[]");
                    }
                    if (reasoningSteps.isEmpty()) {
                        reasoningSteps.append("[]");
                    }
                    messageService.update(assistantMessage.getId(), MessageUpdateRequestDto.builder()
                            .content(fullAnswer.toString())
                            .source(source.toString())
                            .suggestedReplies(suggestedReplies.toString())
                            .reasoningSteps(reasoningSteps.toString())
                            .build());

                    asyncUpdateTopicSummary(finalTopicId);
                })
                .doOnError(e -> log.error("Error during streaming", e))
                .doFinally(signalType -> log.info("Streaming completed with signal: {}", signalType));
    }

    /**
     * Chat with noteBook, if noteBookId is null, create new noteBook and chat, else
     * chat with exist noteBook
     * 
     * @param noteBookId
     * @param requestDto
     * @return
     * @throws JsonProcessingException
     */
    public Flux<String> chatNoteBook(UUID noteBookId, NoteBookCreateConversationRequestDto requestDto)
            throws JsonProcessingException {
        // If noteBook not exists, create new noteBook
        if (noteBookId == null)
            noteBookId = noteBookService.create(NoteBookCreateRequestDto.builder()
                    .title(requestDto.getMessage())
                    .build()).getId();
        else
            noteBookService.validateNoteBookOfUser(noteBookId, JwtUtil.getUserId());

        UUID finalNoteBookId = noteBookId;

        noteBookService.validateNoteBookId(finalNoteBookId);
        NoteBookEntity noteBookEntity = noteBookService.getEntityById(finalNoteBookId);

        MessageFilterDto messageFilterDto = new MessageFilterDto();
        messageFilterDto.setTypes(Arrays.asList(MessageType.USER.getValue(), MessageType.ASSISTANT.getValue()));
        messageFilterDto.setPageNumber(0);
        messageFilterDto.setPageSize(noteBookRecentMessageWindow());
        messageFilterDto.setSortBy("createdAt");
        messageFilterDto.setSortDir("desc");

        // Query history
        List<RagCompletionRequestDto.Message> historyConversations = messageService
                .getAll(finalNoteBookId, MessageParentType.NOTEBOOK, messageFilterDto).getSecond()
                .stream()
                .map(messageResponseDto -> createRagMessage(messageResponseDto.getType(),
                        messageResponseDto.getContent()))
                .collect(Collectors.toList());

        Collections.reverse(historyConversations);

        historyConversations.add(createRagMessage(MessageType.USER.getValue(), requestDto.getMessage()));

        // Insert user question
        messageService.create(
                finalNoteBookId,
                MessageParentType.NOTEBOOK,
                MessageCreateRequestDto.builder()
                        .content(requestDto.getMessage())
                        .type(MessageType.USER.getValue())
                        .build());

        // Insert assistant question
        MessageResponseDto assistantMessage = messageService.create(
                finalNoteBookId,
                MessageParentType.NOTEBOOK,
                MessageCreateRequestDto.builder()
                        .content("Answering.....")
                        .type(MessageType.ASSISTANT.getValue())
                        .build());

        NotebookRagCompletionRequestDto.Metadata metadata = new NotebookRagCompletionRequestDto.Metadata();
        metadata.setUserId(JwtUtil.getUserId());
        metadata.setOrganizationId(JwtUtil.getOrgId());
        metadata.setNotebookId(finalNoteBookId);
        metadata.setFileIds(requestDto.getSourceIds());
        metadata.setSummaries(buildSummaryMetadata(noteBookEntity));
        metadata.setUserInstruction(noteBookEntity.getInstruction());
        metadata.setScopes(requestDto.getScopes());

        NotebookRagCompletionRequestDto ragCompletionRequestDto = applyAiSettings(NotebookRagCompletionRequestDto.builder()
                .messages(historyConversations)
                .metadata(metadata)
                .stream(true))
                .build();

        StringBuilder fullAnswer = new StringBuilder();
        StringBuilder source = new StringBuilder();
        StringBuilder reasoningSteps = new StringBuilder();
        StringBuilder suggestedReplies = new StringBuilder();

        return ragApiService.noteBookChat(ragCompletionRequestDto)
                .startWith(String.format("{\"messageId\": \"%s\"}", assistantMessage.getId()))
                .startWith(String.format("{\"noteBookId\": \"%s\"}", noteBookId))
                // Trả thêm về dto assistant message luôn
                .startWith(String.format("{\"assistantMessage\": %s}",
                        objectMapper.writeValueAsString(assistantMessage)))
                .doOnNext(raw -> {
                    try {
                        JsonNode node = objectMapper.readTree(raw);

                        if (!node.has("event")) {
                            return;
                        }

                        // Stream theo event (giống chatTopic): final_answer/sources/suggested_replies
                        switch (node.get("event").asText()) {
                            case "delta" -> {
                                if (node.has("content")) {
                                    fullAnswer.append(node.get("content").asText());
                                }
                            }
                            case "final_answer" -> {
                                if (node.has("reasoning_steps")) {
                                    reasoningSteps.setLength(0);
                                    reasoningSteps.append(node.get("reasoning_steps").toString());
                                }

                                if (node.has("sources")) {
                                    source.setLength(0);
                                    source.append(node.get("sources").toString());
                                }

                                if (node.has("content")) {
                                    fullAnswer.setLength(0);
                                    fullAnswer.append(node.get("content").asText());
                                }
                            }
                            case "sources" -> {
                                if (node.has("sources")) {
                                    source.setLength(0);
                                    source.append(node.get("sources").toString());
                                }
                            }
                            case "suggested_replies" -> {
                                JsonNode repliesNode = node.has("suggested_replies")
                                        ? node.get("suggested_replies")
                                        : node.get("content");
                                if (repliesNode != null && !repliesNode.isMissingNode()) {
                                    suggestedReplies.setLength(0);
                                    suggestedReplies.append(normalizeJsonArrayText(repliesNode));
                                }
                            }
                            // Dự phòng trường fullAnswer bị rỗng, thì sẽ lấy content từ event "done" (nếu có) để update lại fullAnswer
                            case "done" -> {
                                if (node.has("content") && fullAnswer.isEmpty()) {
                                    fullAnswer.setLength(0);
                                    fullAnswer.append(node.get("content").asText());
                                }
                            }
                            default -> {
                                // "delta", "done", ... không cần xử lý
                            }
                        }
                    } catch (JsonProcessingException e) {
                        log.error("Fail to parse stream token", e);
                    }
                })
                .doOnComplete(() -> {
                    if (source.isEmpty()) {
                        source.append("[]");
                    }
                    if (reasoningSteps.isEmpty()) {
                        reasoningSteps.append("[]");
                    }
                    if (suggestedReplies.isEmpty()) {
                        suggestedReplies.append("[]");
                    }
                    messageService.update(assistantMessage.getId(), MessageUpdateRequestDto.builder()
                            .content(fullAnswer.toString())
                            .source(source.toString())
                            .reasoningSteps(reasoningSteps.toString())
                            .suggestedReplies(suggestedReplies.toString())
                            .build());

                    asyncUpdateNoteBookSummary(finalNoteBookId);
                });
    }

    /**
     * Lấy metadata các loại tài liệu hỗ trợ cho draft, quản lý tập trung từ RAG service.
     * @return danh sách document types
     */
    public List<RagDraftDocumentTypeDto> getDraftDocumentTypes() {
        return ragApiService.getDraftDocumentTypes();
    }

    /**
     * Lấy metadata các chuẩn định dạng văn bản hỗ trợ cho draft, quản lý tập trung từ RAG service.
     * @return danh sách format standards
     */
    public List<RagDraftFormatStandardDto> getDraftFormatStandards() {
        return ragApiService.getDraftFormatStandards();
    }

    public Flux<String> draftCreate(DraftResponseDto draftResponse, Set<String> fileIds) throws JsonProcessingException {
        UUID capturedUserId = JwtUtil.getUserId();
        UUID capturedOrgId = JwtUtil.getOrgId();

        // Insert assistant placeholder
        MessageResponseDto assistantMessage = messageService.create(
                draftResponse.getId(),
                MessageParentType.DRAFT,
                MessageCreateRequestDto.builder()
                        .content("Thinking .....")
                        .type(MessageType.ASSISTANT.getValue())
                        .build());

        DraftRagCreateRequestDto draftRagCreateRequestDto = DraftRagCreateRequestDto.builder()
                .user_request(draftResponse.getTitle())
                .document_type(draftResponse.getType())
                .format_standard(draftResponse.getFormatStandard())
                .context(draftResponse.getDetailedDescription())
                .userId(capturedUserId)
                .organizationId(capturedOrgId)
                .scopes(draftResponse.getScopes() != null
                        ? Set.copyOf(draftResponse.getScopes())
                        : Set.of())
                .fileIds(fileIds)
                .stream(true)
                .max_iterations(DRAFT_CREATE_MAX_ITERATIONS)
                .build();

        StringBuilder sessionId = new StringBuilder();
        StringBuilder questionForUser = new StringBuilder();
        StringBuilder draftContent = new StringBuilder();
        StringBuilder sources = new StringBuilder();

        return ragApiService.draftCreate(draftRagCreateRequestDto)
                .startWith(String.format("{\"messageId\": \"%s\"}", assistantMessage.getId()))
                .startWith(String.format("{\"draftId\": \"%s\"}", draftResponse.getId()))
                .startWith(String.format("{\"assistantMessage\": %s}",
                        objectMapper.writeValueAsString(assistantMessage)))
                .doOnNext(raw -> {
                    try {
                        JsonNode node = objectMapper.readTree(raw);

                        if (!node.has("event")) {
                            return;
                        }

                        if (node.has("session_id")) {
                            sessionId.setLength(0);
                            sessionId.append(node.get("session_id").asText());
                        }

                        switch (node.get("event").asText()) {
                            case "draft_produced" -> {
                                if (node.has("content")) {
                                    draftContent.append(node.get("content").asText());
                                }
                            }
                            case "draft_revised" -> {
                                if (node.has("content")) {
                                    draftContent.append(node.get("content").asText());
                                }
                            }
                            case "awaiting_input" -> {
                                if (node.has("content")) {
                                    questionForUser.setLength(0);
                                    questionForUser.append(node.get("content").asText());
                                }
                            }
                            case "final_answer" -> {
                                if (node.has("session_id")) {
                                    sessionId.setLength(0);
                                    sessionId.append(node.get("session_id").asText());
                                }

                                if (node.has("content")) {
                                    draftContent.setLength(0);
                                    draftContent.append(node.get("content").asText());
                                }

                                if (node.has("sources")) {
                                    sources.setLength(0);
                                    sources.append(node.get("sources").toString());
                                }

                            }
                            default -> {
                                // Các event khác không cần xử lý
                            }
                        }
                    } catch (JsonProcessingException e) {
                        log.error("Fail to parse stream token", e);
                    }
                })
                .doOnComplete(() -> {
                    // Update sessionId for draft
                    String sessionIdStr = sessionId.toString();
                    draftService.updateSessionId(draftResponse.getId(), sessionIdStr);

                    // Update assistant message with generated content
                    if(questionForUser.isEmpty()) {
                        questionForUser.append("Đã hoàn thành");
                    } else {
                        draftContent.setLength(0);
                    }

                    if (sources.isEmpty()) {
                        sources.append("[]");
                    }

                    messageService.update(assistantMessage.getId(), MessageUpdateRequestDto.builder()
                            .content(questionForUser.toString())
                            .source(sources.toString())
                            .build());

                    // Lưu thành 1 version mới của draft để theo dõi lịch sử chỉnh sửa nếu có
                    // draftContent
                    String draftContentStr = draftContent.toString();
                    if (!draftContentStr.isEmpty()) {
                        DraftVersionResponseDto newVersion = draftService.saveVersion(
                                draftResponse.getId(),
                                DraftSaveVersionRequestDto.builder()
                                        .currentDraftContent(draftContentStr)
                                        .changeRequest(null)
                                        .build());
                        log.info("Draft {} updated to version {} via chat", draftResponse.getId(), newVersion.getVersionNumber());
                    }
                })
                .concatWith(
                        Mono.fromCallable(() -> {
                            assistantMessage.setContent(questionForUser.toString());
                            assistantMessage.setSource(sources.toString());
                            return String.format(
                                    "{\"updatedAssistantMessage\": %s}",
                                    objectMapper.writeValueAsString(assistantMessage));
                        }).flatMapMany(Flux::just))
                .doOnError(e -> {
                    log.error("Error during draft chat streaming", e);
                })
                .doFinally(signalType -> log.info("Draft chat streaming completed with signal: {}", signalType));
    }

    /**
     * Chat with draft, draft is a special type that only user can see, used for
     * user to iteratively edit a piece of content via chatting with AI. If draftId
     * is null, create new draft and chat, else chat with exist draft
     * 
     * @param draftId
     * @param requestDto
     * @return
     * @throws JsonProcessingException
     */
    public Flux<String> chatDraft(UUID draftId, DraftChatRequestDto requestDto, Set<String> fileIds) throws JsonProcessingException {
        UUID capturedUserId = JwtUtil.getUserId();

        draftService.validateDraftOfUser(draftId, capturedUserId);
        DraftEntity draftEntity = draftService.getEntityById(draftId);

        // Insert user message
        messageService.create(
                draftId,
                MessageParentType.DRAFT,
                MessageCreateRequestDto.builder()
                        .content(requestDto.getMessage())
                        .type(MessageType.USER.getValue())
                        .build());

        // Insert assistant placeholder
        MessageResponseDto assistantMessage = messageService.create(
                draftId,
                MessageParentType.DRAFT,
                MessageCreateRequestDto.builder()
                        .content("Thinking .....")
                        .type(MessageType.ASSISTANT.getValue())
                        .build());

        DraftRagReviseRequestDto draftRagReviseRequestDto = DraftRagReviseRequestDto.builder()
                .session_id(draftEntity.getSessionId())
                .feedback(requestDto.getMessage())
                .scopes(requestDto.getScopes())
                .fileIds(fileIds)
                .stream(true)
                .max_iterations(DRAFT_REVISE_MAX_ITERATIONS)
                .build();

        StringBuilder status = new StringBuilder();
        StringBuilder questionForUser = new StringBuilder();
        StringBuilder draftContent = new StringBuilder();
        StringBuilder sources = new StringBuilder();

        return ragApiService.draftRevise(draftRagReviseRequestDto)
                .startWith(String.format("{\"messageId\": \"%s\"}", assistantMessage.getId()))
                .startWith(String.format("{\"draftId\": \"%s\"}", draftId))
                .startWith(String.format("{\"assistantMessage\": %s}",
                        objectMapper.writeValueAsString(assistantMessage)))
                .doOnNext(raw -> {
                    try {
                        JsonNode node = objectMapper.readTree(raw);
                        
                        if (!node.has("event")) {
                            return;
                        }

                        switch (node.get("event").asText()) {
                            case "draft_produced" -> {
                                if (node.has("content")) {
                                    draftContent.append(node.get("content").asText());
                                }
                            }
                            case "draft_revised" -> {
                                if (node.has("content")) {
                                    draftContent.append(node.get("content").asText());
                                }
                            }
                            case "question_for_user" -> {
                                if (node.has("content")) {
                                    questionForUser.setLength(0);
                                    questionForUser.append(node.get("content").asText());
                                }
                            }
                            case "final_answer" -> {
                                if (node.has("status")) {
                                    status.setLength(0);
                                    status.append(node.get("status").asText());
                                }

                                if (node.has("content")) {
                                    draftContent.setLength(0);
                                    draftContent.append(node.get("content").asText());
                                }

                                if (node.has("sources")) {
                                    sources.setLength(0);
                                    sources.append(node.get("sources").asText());
                                }
                            }
                            default -> {
                                // Các event khác không cần xử lý
                            }
                        }
                    } catch (JsonProcessingException e) {
                        log.error("Fail to parse stream token", e);
                    }
                })
                .doOnComplete(() -> {
                    // Update assistant message with generated content
                    if(questionForUser.isEmpty()) {
                        questionForUser.append("Đã hoàn thành");
                    } else {
                        draftContent.setLength(0);
                    }   

                    if (sources.isEmpty()) {
                        sources.append("[]");
                    }

                    messageService.update(assistantMessage.getId(), MessageUpdateRequestDto.builder()
                            .content(questionForUser.toString())
                            .source(sources.toString())
                            .build());

                    // Lưu thành 1 version mới của draft để theo dõi lịch sử chỉnh sửa nếu có
                    // draftContent
                    String draftContentStr = draftContent.toString();
                    if (!draftContentStr.isEmpty()) {
                        DraftVersionResponseDto newVersion = draftService.saveVersion(
                                draftId,
                                DraftSaveVersionRequestDto.builder()
                                        .currentDraftContent(draftContentStr)
                                        .changeRequest(requestDto.getMessage())
                                        .build());
                        log.info("Draft {} updated to version {} via chat", draftId, newVersion.getVersionNumber());
                    }
                })
                .concatWith(
                        Mono.fromCallable(() -> {
                            assistantMessage.setContent(questionForUser.toString());
                            assistantMessage.setSource(sources.toString());
                            return String.format(
                                    "{\"updatedAssistantMessage\": %s}",
                                    objectMapper.writeValueAsString(assistantMessage));
                        }).flatMapMany(Flux::just))
                .doOnError(e -> log.error("Error during draft chat streaming", e))
                .doFinally(signalType -> log.info("Draft chat streaming completed with signal: {}", signalType));
    }

    /**
     * Async: generate a better title for a newly created topic via AI, update DB,
     * and notify FE via SSE.
     * Must capture orgId and userId before spawning the thread (JWT context is
     * thread-local).
     */
    public void asyncUpdateTopicTitle(UUID topicId, UUID orgId, UUID userId, String input) {
        CompletableFuture.runAsync(() -> {
            try {
                String betterTitle = generalTitleOfTopic(input);
                topicService.updateTitleInternal(topicId, betterTitle);
                systemEventSseService.publish(
                        orgId,
                        userId,
                        SystemEventType.TOPIC_TITLE_UPDATED,
                        SystemEventSource.TOPIC,
                        Map.of("topicId", topicId.toString(), "title", betterTitle));
            } catch (JsonProcessingException | RuntimeException e) {
                log.error("Failed to generate AI title for topic {}", topicId, e);
            }
        });
    }

    public void asyncUpdateTopicSummary(UUID topicId) {
        CompletableFuture.runAsync(() -> {
            try {
                TopicEntity topicEntity = topicService.getEntityById(topicId);
                List<MessageResponseDto> topicMessages = messageService.getTopicMessagesAfterInternal(
                        topicId,
                        topicEntity.getConversationSummaryLastMessageId());

                int recentWindow = topicRecentMessageWindow();
                if (!shouldSummarize(topicMessages.size(), recentWindow)) {
                    return;
                }

                int summarizeUntilIndex = topicMessages.size() - recentWindow;
                List<MessageResponseDto> messagesToSummarize = topicMessages.subList(0, summarizeUntilIndex);

                SummaryResult result = summarize(
                        topicEntity.getConversationSummary(),
                        messagesToSummarize,
                        AiPromptTemplates::topicSummaryPrompt);

                if (result == null) {
                    return;
                }

                topicService.updateConversationSummaryInternal(topicId, result.summary(), result.checkpointMessageId());
            } catch (JsonProcessingException | RuntimeException e) {
                log.error("Failed to generate conversation summary for topic {}", topicId, e);
            }
        });
    }

    public void asyncUpdateNoteBookSummary(UUID noteBookId) {
        CompletableFuture.runAsync(() -> {
            try {
                NoteBookEntity noteBookEntity = noteBookService.getEntityById(noteBookId);
                List<MessageResponseDto> noteBookMessages = messageService.getNoteBookMessagesAfterInternal(
                        noteBookId,
                        noteBookEntity.getConversationSummaryLastMessageId());

                int recentWindow = noteBookRecentMessageWindow();
                if (!shouldSummarize(noteBookMessages.size(), recentWindow)) {
                    return;
                }

                int summarizeUntilIndex = noteBookMessages.size() - recentWindow;
                List<MessageResponseDto> messagesToSummarize = noteBookMessages.subList(0, summarizeUntilIndex);

                SummaryResult result = summarize(
                        noteBookEntity.getConversationSummary(),
                        messagesToSummarize,
                        AiPromptTemplates::noteBookSummaryPrompt);

                if (result == null) {
                    return;
                }

                noteBookService.updateConversationSummaryInternal(noteBookId, result.summary(), result.checkpointMessageId());
            } catch (JsonProcessingException | RuntimeException e) {
                log.error("Failed to generate conversation summary for notebook {}", noteBookId, e);
            }
        });
    }

    /**
     * Nén một lô tin nhắn thành summary mới và xác định checkpoint đi kèm.
     * <p>
     * Checkpoint là id của tin nhắn <b>cuối cùng thực sự được đưa vào prompt</b>. Khi tổng
     * nội dung vượt ngân sách {@code ai.summary.maxInputChars}, các tin nhắn cũ nhất bị lược
     * bớt; nếu vẫn tiến checkpoint tới tin nhắn cuối của cả lô thì phần bị lược sẽ mất ngữ
     * cảnh vĩnh viễn. Vì vậy checkpoint chỉ tiến tới tin nhắn cuối của đoạn còn lại.
     *
     * @param existingSummary  summary hiện có (có thể null/rỗng)
     * @param messages         các tin nhắn mới cần hấp thụ
     * @param promptBuilder    hàm tạo prompt theo loại hội thoại (Topic/Notebook)
     * @return {@link SummaryResult} hoặc {@code null} nếu model không trả về nội dung
     * @throws JsonProcessingException lỗi parse response từ RAG service
     */
    private SummaryResult summarize(String existingSummary, List<MessageResponseDto> messages,
            PromptBuilder promptBuilder) throws JsonProcessingException {
        if (messages == null || messages.isEmpty()) {
            return null;
        }

        ConversationBlock block = buildConversationBlock(messages);
        // Không có nội dung nào để hấp thụ (mọi tin nhắn đều rỗng) → bỏ qua, giữ nguyên
        // checkpoint và summary cũ thay vì gọi model vô ích rồi ghi checkpoint null.
        if (block.lastMessageId() == null) {
            return null;
        }

        String prompt = promptBuilder.build(normalizeExistingSummary(existingSummary), block.text(), summaryMaxWords());

        String summary = generateSanitizedString(buildSimpleCompletionRequest(prompt), false, summaryMaxWords());
        if (summary == null) {
            return null;
        }

        return new SummaryResult(summary, block.lastMessageId());
    }

    /** Hàm tạo prompt cho loại hội thoại tương ứng. */
    @FunctionalInterface
    private interface PromptBuilder {
        String build(String existingSummary, String messageBlock, int targetWords);
    }

    /**
     * Kết quả nén hội thoại: nội dung summary mới và checkpoint tương ứng.
     *
     * @param summary            nội dung summary đã làm sạch
     * @param checkpointMessageId id tin nhắn cuối cùng đã được hấp thụ
     */
    private record SummaryResult(String summary, UUID checkpointMessageId) {
    }

    /**
     * Block hội thoại đã chuẩn hóa để đưa vào prompt.
     *
     * @param text          nội dung block
     * @param lastMessageId id tin nhắn cuối cùng có mặt trong block ({@code null} nếu rỗng)
     */
    private record ConversationBlock(String text, UUID lastMessageId) {
    }

    /**
     * Sinh tiêu đề cho note dựa trên nội dung.
     * <p>
     * Input được cắt ngắn theo {@code ai.title.maxInputChars} (mặc định
     * {@value #DEFAULT_TITLE_MAX_INPUT_CHARS} ký tự) để tránh gửi toàn bộ nội dung dài
     * lên model nhỏ, giúp giảm độ trễ và tránh nhiễu. Kết quả được làm sạch
     * (bỏ dấu nháy, tiền tố "Title:", khối suy luận) trước khi trả về.
     *
     * @param input nội dung note
     * @return tiêu đề đã làm sạch, hoặc {@code null} nếu model không trả về nội dung
     * @throws JsonProcessingException lỗi parse response từ RAG service
     */
    public String generalTitleOfNote(String input) throws JsonProcessingException {
        String prompt = AiPromptTemplates.titlePrompt(
                "một ghi chú",
                "tiêu đề PHẢI cùng ngôn ngữ với nội dung ghi chú.",
                truncateForPrompt(input, titleMaxInputChars()));

        return generateSanitizedString(buildSimpleCompletionRequest(prompt), true, TITLE_MAX_WORDS);
    }

    /**
     * Sinh tiêu đề cho topic chat dựa trên câu hỏi đầu tiên của người dùng.
     * <p>
     * Input được cắt ngắn theo {@code ai.title.maxInputChars}. Kết quả được làm sạch
     * (bỏ dấu nháy, tiền tố "Title:", khối suy luận) trước khi trả về.
     *
     * @param input nội dung người dùng nhập
     * @return tiêu đề đã làm sạch, hoặc {@code null} nếu model không trả về nội dung
     * @throws JsonProcessingException lỗi parse response từ RAG service
     */
    public String generalTitleOfTopic(String input) throws JsonProcessingException {
        String prompt = AiPromptTemplates.titlePrompt(
                "một cuộc hội thoại chat",
                "tiêu đề PHẢI cùng ngôn ngữ với nội dung người dùng nhập.",
                truncateForPrompt(input, titleMaxInputChars()));

        return generateSanitizedString(buildSimpleCompletionRequest(prompt), true, TITLE_MAX_WORDS);
    }

    /**
     * Nén hội thoại Topic thành rolling summary.
     * <p>
     * Tin nhắn được chuẩn hóa (bỏ khối JSON lồng như source/reasoning của message
     * assistant) và cắt theo ngân sách ký tự {@code ai.summary.maxInputChars} để tránh
     * đẩy payload khổng lồ lên model nhỏ. Prompt yêu cầu trả về DUY NHẤT nội dung
     * summary (không kèm phần suy luận) — cần thiết cho các reasoning model như
     * {@code gpt-oss-20b}.
     *
     * @param existingSummary summary hiện có (có thể null/rỗng)
     * @param messages        các tin nhắn mới cần hấp thụ
     * @return summary đã làm sạch, hoặc {@code null} nếu model không trả về nội dung
     * @throws JsonProcessingException lỗi parse response từ RAG service
     */
    public String generalSummaryOfTopic(String existingSummary, List<MessageResponseDto> messages)
            throws JsonProcessingException {
        SummaryResult result = summarize(existingSummary, messages, AiPromptTemplates::topicSummaryPrompt);
        return result == null ? null : result.summary();
    }

    /**
     * Nén hội thoại Notebook thành rolling summary.
     * <p>
     * Cùng cơ chế với {@link #generalSummaryOfTopic(String, List)} nhưng ưu tiên các
     * thông tin đặc thù NotebookLM (yêu cầu, kế hoạch, tham chiếu tài liệu nguồn).
     *
     * @param existingSummary summary hiện có (có thể null/rỗng)
     * @param messages        các tin nhắn mới cần hấp thụ
     * @return summary đã làm sạch, hoặc {@code null} nếu model không trả về nội dung
     * @throws JsonProcessingException lỗi parse response từ RAG service
     */
    public String generalSummaryOfNoteBook(String existingSummary, List<MessageResponseDto> messages)
            throws JsonProcessingException {
        SummaryResult result = summarize(existingSummary, messages, AiPromptTemplates::noteBookSummaryPrompt);
        return result == null ? null : result.summary();
    }

    /**
     * Tạo request completion "thuần AI" (không metadata đặc trưng) cho các tác vụ
     * phụ trợ như sinh tiêu đề / nén hội thoại.
     * <p>
     * Dùng UUID ngẫu nhiên cho {@code user_id}/{@code organization_id} vì đây là tác vụ
     * nội bộ, không truy vấn dữ liệu theo quyền của người dùng (scopes chỉ gồm
     * {@code personal}). Các request này cũng không stream.
     *
     * @param prompt prompt hoàn chỉnh
     * @return request DTO đã áp dụng AI settings
     */
    private RagCompletionRequestDto buildSimpleCompletionRequest(String prompt) {
        RagCompletionRequestDto.Metadata metadata = new RagCompletionRequestDto.Metadata();
        metadata.setUserId(UUID.randomUUID());
        metadata.setOrganizationId(UUID.randomUUID());
        metadata.setScopes(Set.of(DataScope.PERSONAL.getKey().toLowerCase()));

        return applyAiSettings(RagCompletionRequestDto.builder()
                .messages(List.of(createRagMessage(MessageType.USER.getValue(), prompt)))
                .stream(false), false)
                .metadata(metadata)
                .build();
    }

    /**
     * Gọi {@link #generateString(RagCompletionRequestDto)} rồi làm sạch kết quả.
     * <p>
     * Chặn độ dài đầu ra là rào chắn cuối cùng chống việc model nhỏ phình to output:
     * tiêu đề quá dài làm vỡ layout, summary phình to dần sẽ lấp đầy ngân sách input
     * của các lần nén sau. Giới hạn ký tự tính từ số từ xấp xỉ (một từ tiếng Việt/Anh
     * trung bình khoảng {@value #APPROX_CHARS_PER_WORD} ký tự).
     *
     * @param requestDto        request completion
     * @param collapseToOneLine true nếu kết quả phải là một dòng (dùng cho tiêu đề)
     * @param maxWords          số từ tối đa (0 = không giới hạn)
     * @return nội dung đã làm sạch, hoặc {@code null} nếu rỗng
     */
    private String generateSanitizedString(RagCompletionRequestDto requestDto, boolean collapseToOneLine, int maxWords) {
        String raw = generateString(requestDto);
        if (raw == null) {
            return null;
        }

        int maxChars = maxWords > 0 ? maxWords * APPROX_CHARS_PER_WORD : 0;
        return AiTextSanitizer.sanitize(raw, collapseToOneLine, maxChars);
    }

    /**
     * Overload không giới hạn độ dài (dùng cho các tác vụ không cần clamp).
     */
    private String generateSanitizedString(RagCompletionRequestDto requestDto, boolean collapseToOneLine) {
        return generateSanitizedString(requestDto, collapseToOneLine, 0);
    }

    /**
     * Cắt ngắn nội dung đầu vào theo ngân sách ký tự trước khi đưa vào prompt.
     *
     * @param value     nội dung gốc
     * @param maxLength số ký tự tối đa (nếu &lt;= 0 thì bỏ qua giới hạn)
     * @return nội dung đã cắt
     */
    private String truncateForPrompt(String value, int maxLength) {
        return AiTextSanitizer.truncate(value, maxLength);
    }

    /**
     * Chuẩn hóa summary cũ trước khi đưa vào prompt; trả về placeholder khi chưa có.
     */
    private String normalizeExistingSummary(String existingSummary) {
        if (isBlank(existingSummary)) {
            return AiPromptTemplates.NO_EXISTING_SUMMARY;
        }
        return existingSummary.replaceAll("\\s+", " ").trim();
    }

    /**
     * Ghép các tin nhắn mới thành block text dạng {@code Role: nội dung} để đưa vào prompt.
     * <p>
     * Áp dụng hai biện pháp bảo vệ cần thiết cho model nhỏ:
     * <ul>
     *   <li><b>Cắt ngắn từng tin nhắn</b> ({@value #DEFAULT_MESSAGE_MAX_CHARS} ký tự) —
     *       nội dung assistant có thể chứa JSON source/reasoning rất dài.</li>
     *   <li><b>Cắt bớt tin nhắn cũ nhất</b> khi tổng block vượt ngân sách
     *       {@code ai.summary.maxInputChars}, nhưng giữ lại các tin nhắn gần nhất vì
     *       chúng mang ngữ cảnh mới nhất.</li>
     * </ul>
     *
     * @param messages danh sách tin nhắn mới
     * @return block text đã chuẩn hóa
     */
    private ConversationBlock buildConversationBlock(List<MessageResponseDto> messages) {
        if (messages == null || messages.isEmpty()) {
            return new ConversationBlock(AiPromptTemplates.NO_MESSAGES, null);
        }

        int budget = summaryMaxInputChars();
        int perMessageLimit = Math.min(DEFAULT_MESSAGE_MAX_CHARS,
                budget > 0 ? Math.max(1, budget / messages.size()) : DEFAULT_MESSAGE_MAX_CHARS);

        List<MessageResponseDto> prepared = new ArrayList<>(messages.size());
        List<String> lines = new ArrayList<>(messages.size());
        for (MessageResponseDto message : messages) {
            String content = message.getContent() == null ? "" : message.getContent().trim();
            if (content.isEmpty()) {
                continue;
            }
            if (content.length() > perMessageLimit) {
                content = content.substring(0, perMessageLimit) + "...(lược bớt)";
            }
            prepared.add(message);
            lines.add(toDisplayRole(message.getType()) + ": " + content);
        }

        if (lines.isEmpty()) {
            return new ConversationBlock(AiPromptTemplates.NO_MESSAGES, null);
        }

        // Duyệt từ cuối về đầu để ưu tiên tin nhắn gần nhất khi vượt ngân sách
        Deque<String> kept = new ArrayDeque<>();
        int used = 0;
        int keptCount = 0;
        for (int i = lines.size() - 1; i >= 0; i--) {
            String line = lines.get(i);
            int cost = line.length() + 1;
            if (budget > 0 && used + cost > budget && !kept.isEmpty()) {
                break;
            }
            kept.addFirst(line);
            used += cost;
            keptCount++;
        }

        boolean truncated = keptCount < lines.size();
        // Tin nhắn cuối cùng thực sự được đưa vào prompt → dùng làm checkpoint để phần bị
        // lược bớt sẽ được tóm tắt ở lần chạy sau, không bị mất ngữ cảnh vĩnh viễn.
        UUID lastMessageId = prepared.get(keptCount - 1).getId();

        StringBuilder block = new StringBuilder();
        if (truncated) {
            block.append("...(một số tin nhắn cũ đã được lược bớt)\n");
        }
        block.append(String.join("\n", kept));
        return new ConversationBlock(block.toString(), lastMessageId);
    }

    /**
     * Chuyển {@code type} thô của message sang nhãn hiển thị thân thiện để model
     * phân biệt được vai trò người dùng và trợ lý.
     * <p>
     * Trước đây prompt dùng thẳng giá trị thô ({@code "user"} / {@code "assistant"})
     * gây khó hiểu cho các model nhỏ; nhãn {@code User}/{@code Assistant} rõ ràng hơn.
     */
    private String toDisplayRole(String type) {
        if (type == null) {
            return "Unknown";
        }
        return switch (type.trim().toLowerCase()) {
            case "user" -> "User";
            case "assistant" -> "Assistant";
            default -> type;
        };
    }

    private String buildSummaryMetadata(TopicEntity topicEntity) {
        if (topicEntity == null || isBlank(topicEntity.getConversationSummary())) {
            return "";
        }

        return topicEntity.getConversationSummary().replaceAll("\\s+", " ").trim();
    }

    private String buildSummaryMetadata(NoteBookEntity noteBookEntity) {
        if (noteBookEntity == null || isBlank(noteBookEntity.getConversationSummary())) {
            return "";
        }

        return noteBookEntity.getConversationSummary().replaceAll("\\s+", " ").trim();
    }

    private RagCompletionRequestDto.Message createRagMessage(String role, String content) {
        RagCompletionRequestDto.Message message = new RagCompletionRequestDto.Message();
        message.setRole(role);
        message.setContent(content);
        return message;
    }

    /**
     * Định nghĩa khi nào cần gọi API tóm tắt lại cuộc hội thoại để cập nhật
     * summary. Nếu tổng số tin nhắn sau checkpoint (tức là tin nhắn chưa được tóm
     * tắt) vượt quá recentWindow + minMessagesToCompress, thì sẽ gọi API tóm tắt.
     * recentWindow đảm bảo rằng luôn có một số lượng tin nhắn gần đây được giữ
     * nguyên trong summary để duy trì ngữ cảnh tươi mới, trong khi
     * minMessagesToCompress đảm bảo rằng chỉ gọi API tóm tắt khi có đủ tin nhắn mới
     * cần được nén lại, tránh việc gọi API quá thường xuyên với lượng tin nhắn quá
     * ít.
     * 
     * @param totalMessagesAfterCheckpoint tổng số tin nhắn sau checkpoint
     * @param recentWindow                 số lượng tin nhắn gần đây được giữ nguyên
     *                                     trong summary
     * @return true nếu cần tóm tắt, false nếu không
     */
    private boolean shouldSummarize(int totalMessagesAfterCheckpoint, int recentWindow) {
        return totalMessagesAfterCheckpoint > recentWindow + minMessagesToCompress();
    }

    /**
     * Đọc cấu hình từ appProperties, nếu không có hoặc không hợp lệ (null hoặc <=0)
     * thì trả về giá trị mặc định.
     * 
     * @return giá trị cấu hình hợp lệ hoặc giá trị mặc định
     */
    private int topicRecentMessageWindow() {
        return readPositiveMemoryConfig(
                appProperties.getRag() != null && appProperties.getRag().getMemory() != null
                        ? appProperties.getRag().getMemory().getTopicRecentMessageWindow()
                        : null,
                DEFAULT_TOPIC_RECENT_MESSAGE_WINDOW);
    }

    private int noteBookRecentMessageWindow() {
        return readPositiveMemoryConfig(
                appProperties.getRag() != null && appProperties.getRag().getMemory() != null
                        ? appProperties.getRag().getMemory().getNoteBookRecentMessageWindow()
                        : null,
                DEFAULT_NOTEBOOK_RECENT_MESSAGE_WINDOW);
    }

    private int minMessagesToCompress() {
        return readPositiveMemoryConfig(
                appProperties.getRag() != null && appProperties.getRag().getMemory() != null
                        ? appProperties.getRag().getMemory().getMinMessagesToCompress()
                        : null,
                DEFAULT_MIN_MESSAGES_TO_COMPRESS);
    }

    private int readPositiveMemoryConfig(Integer configured, int defaultValue) {
        if (configured == null || configured <= 0) {
            return defaultValue;
        }
        return configured;
    }

    /** Ngân sách ký tự input cho prompt sinh tiêu đề (setting {@code ai.title.maxInputChars}). */
    private int titleMaxInputChars() {
        return readPositiveConfig(systemSettingService.getString("ai.title.maxInputChars", null),
                DEFAULT_TITLE_MAX_INPUT_CHARS);
    }

    /** Ngân sách ký tự input cho prompt nén summary (setting {@code ai.summary.maxInputChars}). */
    private int summaryMaxInputChars() {
        return readPositiveConfig(systemSettingService.getString("ai.summary.maxInputChars", null),
                DEFAULT_SUMMARY_MAX_INPUT_CHARS);
    }

    /** Ngân sách từ cho summary đầu ra (setting {@code ai.summary.maxWords}). */
    private int summaryMaxWords() {
        return readPositiveConfig(systemSettingService.getString("ai.summary.maxWords", null),
                DEFAULT_SUMMARY_MAX_WORDS);
    }

    /**
     * Đọc cấu hình số nguyên dạng chuỗi; trả về mặc định nếu null/không parse được/&lt;= 0.
     */
    private int readPositiveConfig(String configured, int defaultValue) {
        if (isBlank(configured)) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(configured.trim());
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException exception) {
            log.warn("Giá trị cấu hình không hợp lệ '{}', sử dụng mặc định {}", configured, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Áp dụng cấu hình AI từ system settings vào một {@code SuperBuilder} builder.
     * <p>
     * Generic {@code T extends RagCompletionRequestDtoBuilder<?, ?>, B extends B} cho phép
     * dùng chung cho base lớp {@link RagCompletionRequestDto} lẫn các subclass
     * ({@link TopicRagCompletionRequestDto}, {@link NotebookRagCompletionRequestDto})
     * mà không cần overload, đồng thời giữ đúng kiểu builder trả về.
     * Đọc các settings:
     * <ul>
     * <li>{@code ai.model} — model AI mặc định (ví dụ: gpt-4)</li>
     * <li>{@code ai.temperature} — nhiệt độ sinh (0.0 - 2.0)</li>
     * <li>{@code ai.maxTokens} — số token tối đa mỗi request</li>
     * </ul>
     *
     * @param builder builder super-builder của DTO
     * @param <B> kiểu builder (self-type)
     * @return builder đã được apply AI settings
     */
    private <B extends RagCompletionRequestDto.RagCompletionRequestDtoBuilder<?, ?>> B applyAiSettings(B builder) {
        return applyAiSettings(builder, true);
    }

    /**
     * Áp dụng cấu hình AI từ system settings vào builder, có thể bỏ qua temperature.
     * <p>
     * Các tác vụ phụ trợ cần kết quả <b>tất định</b> (sinh tiêu đề, nén summary) nên
     * không dùng temperature của chat: temperature cao khiến model nhỏ sinh tiêu đề
     * lan man, thêm lời dẫn hoặc bịa chi tiết. Vì vậy các tác vụ này override
     * temperature về {@value #DETERMINISTIC_TEMPERATURE} và chặn phần suy luận dài;
     * {@code ai.model} và {@code ai.maxTokens} vẫn được áp dụng bình thường.
     *
     * @param builder            builder super-builder của DTO
     * @param applyConfiguredTemperature {@code false} để force temperature tất định
     * @param <B>                kiểu builder (self-type)
     * @return builder đã được apply AI settings
     */
    private <B extends RagCompletionRequestDto.RagCompletionRequestDtoBuilder<?, ?>> B applyAiSettings(
            B builder, boolean applyConfiguredTemperature) {
        String model = systemSettingService.getString("ai.model", "");
        if (!isBlank(model)) {
            builder.model(model);
        }
        if (applyConfiguredTemperature) {
            double temperature = systemSettingService.getDouble("ai.temperature", -1);
            if (temperature >= 0) {
                builder.temperature(temperature);
            }
        } else {
            builder.temperature(DETERMINISTIC_TEMPERATURE);
        }
        int maxTokens = systemSettingService.getInt("ai.maxTokens", -1);
        if (maxTokens > 0) {
            builder.maxTokens(maxTokens);
        }

        log.info("Applied AI settings: model={}, temperatureApplied={}, maxTokens={}",
                model, applyConfiguredTemperature, maxTokens);

        return builder;
    }

    /**
     * Chuẩn hóa một JsonNode thành text dạng mảng JSON (vd: ["a", "b"]).
     * Xử lý cả 2 trường hợp:
     * <ul>
     * <li>node là mảng JSON thật → trả về toString() trực tiếp</li>
     * <li>node là chuỗi chứa JSON bị double-encoded (vd: "[\"a\", \"b\"]") →
     * parse lại để lưu DB đúng dạng array, tránh lưu thành string lồng array</li>
     * </ul>
     *
     * @param node node cần chuẩn hóa
     * @return text dạng mảng JSON
     */
    private String normalizeJsonArrayText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "[]";
        }

        if (node.isArray()) {
            return node.toString();
        }

        if (node.isTextual()) {
            String text = node.asText().trim();
            try {
                JsonNode parsed = objectMapper.readTree(text);
                if (parsed.isArray()) {
                    return parsed.toString();
                }
            } catch (JsonProcessingException e) {
                log.warn("Không parse được JSON array từ text: {}", text);
            }
        }

        return node.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

}
