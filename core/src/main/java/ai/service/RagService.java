package ai.service;

import java.util.Arrays;
import java.util.Collections;
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
import ai.dto.outer.rag.request.RagCompletionRequestDto;
import ai.dto.outer.rag.request.RagDraftCreateRequestDto;
import ai.dto.outer.rag.request.RagDraftReviseRequestDto;
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

        RagCompletionRequestDto.Metadata metadata = new RagCompletionRequestDto.Metadata();
        metadata.setUserId(JwtUtil.getUserId());
        metadata.setOrganizationId(JwtUtil.getOrgId());
        metadata.setTopic_id(finalTopicId);
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

        RagCompletionRequestDto ragCompletionRequestDto = applyAiSettings(RagCompletionRequestDto.builder()
                .messages(historyConversations)
                .metadata(metadata)
                .stream(true))
                .build();

        StringBuilder reasoningSteps = new StringBuilder();
        StringBuilder fullAnswer = new StringBuilder();
        StringBuilder source = new StringBuilder();
        StringBuilder suggestedReplies = new StringBuilder();

        System.out.println(new ObjectMapper().writeValueAsString(ragCompletionRequestDto));

        return ragApiService.topicChat(ragCompletionRequestDto)
                .startWith(String.format("{\"messageId\": \"%s\"}", assistantMessage.getId()))
                .startWith(String.format("{\"topicId\": \"%s\"}", topicId))
                // Trả thêm về dto assistant message luôn
                .startWith(String.format("{\"assistantMessage\": %s}",
                        new ObjectMapper().writeValueAsString(assistantMessage)))
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

        RagCompletionRequestDto.Metadata metadata = new RagCompletionRequestDto.Metadata();
        metadata.setUserId(JwtUtil.getUserId());
        metadata.setOrganizationId(JwtUtil.getOrgId());
        metadata.setNotebook_id(finalNoteBookId);
        metadata.setFileIds(requestDto.getSourceIds());
        metadata.setSummaries(buildSummaryMetadata(noteBookEntity));
        metadata.setUserInstruction(noteBookEntity.getInstruction());
        metadata.setScopes(Set.of(DataScope.PERSONAL.getKey().toLowerCase()));

        RagCompletionRequestDto ragCompletionRequestDto = applyAiSettings(RagCompletionRequestDto.builder()
                .messages(historyConversations)
                .metadata(metadata)
                .stream(true))
                .build();

        StringBuilder fullAnswer = new StringBuilder();
        StringBuilder source = new StringBuilder();
        StringBuilder reasoningSteps = new StringBuilder();
        StringBuilder suggestedReplies = new StringBuilder();

        System.out.println(new ObjectMapper().writeValueAsString(ragCompletionRequestDto));

        return ragApiService.noteBookChat(ragCompletionRequestDto)
                .startWith(String.format("{\"messageId\": \"%s\"}", assistantMessage.getId()))
                .startWith(String.format("{\"noteBookId\": \"%s\"}", noteBookId))
                // Trả thêm về dto assistant message luôn
                .startWith(String.format("{\"assistantMessage\": %s}",
                        new ObjectMapper().writeValueAsString(assistantMessage)))
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

    public Flux<String> draftCreate(DraftResponseDto draftResponse) throws JsonProcessingException {
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

        RagDraftCreateRequestDto ragDraftCreateRequestDto = RagDraftCreateRequestDto.builder()
                .user_request(draftResponse.getTitle())
                .document_type(draftResponse.getType())
                .format_standard(draftResponse.getFormatStandard())
                .context(draftResponse.getDetailedDescription())
                .userId(capturedUserId)
                .organizationId(capturedOrgId)
                .scopes(Set.of(DataScope.PERSONAL.getKey().toLowerCase()))
                .fileIds(Set.of())
                .stream(true)
                .max_iterations(DRAFT_CREATE_MAX_ITERATIONS)
                .build();

        StringBuilder sessionId = new StringBuilder();
        StringBuilder questionForUser = new StringBuilder();
        StringBuilder draftContent = new StringBuilder();
        StringBuilder sources = new StringBuilder();

        return ragApiService.draftCreate(ragDraftCreateRequestDto)
                .startWith(String.format("{\"messageId\": \"%s\"}", assistantMessage.getId()))
                .startWith(String.format("{\"draftId\": \"%s\"}", draftResponse.getId()))
                .startWith(String.format("{\"assistantMessage\": %s}",
                        new ObjectMapper().writeValueAsString(assistantMessage)))
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
                            log.info("assistantMessage before sending: {}", assistantMessage);
                            return String.format(
                                    "{\"updatedAssistantMessage\": %s}",
                                    new ObjectMapper().writeValueAsString(assistantMessage));
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
    public Flux<String> chatDraft(UUID draftId, DraftChatRequestDto requestDto) throws JsonProcessingException {
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

        RagDraftReviseRequestDto ragDraftReviseRequestDto = RagDraftReviseRequestDto.builder()
                .session_id(draftEntity.getSessionId())
                .feedback(requestDto.getMessage())
                .stream(true)
                .max_iterations(DRAFT_REVISE_MAX_ITERATIONS)
                .build();

        StringBuilder status = new StringBuilder();
        StringBuilder questionForUser = new StringBuilder();
        StringBuilder draftContent = new StringBuilder();
        StringBuilder sources = new StringBuilder();

        return ragApiService.draftRevise(ragDraftReviseRequestDto)
                .startWith(String.format("{\"messageId\": \"%s\"}", assistantMessage.getId()))
                .startWith(String.format("{\"draftId\": \"%s\"}", draftId))
                .startWith(String.format("{\"assistantMessage\": %s}",
                        new ObjectMapper().writeValueAsString(assistantMessage)))
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
                            log.info("assistantMessage before sending: {}", assistantMessage);
                            return String.format(
                                    "{\"updatedAssistantMessage\": %s}",
                                    new ObjectMapper().writeValueAsString(assistantMessage));
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
                UUID lastSummarizedMessageId = messagesToSummarize.get(messagesToSummarize.size() - 1).getId();

                String updatedSummary = generalSummaryOfTopic(
                        topicEntity.getConversationSummary(),
                        messagesToSummarize);

                if (updatedSummary == null || updatedSummary.isBlank()) {
                    return;
                }

                topicService.updateConversationSummaryInternal(topicId, updatedSummary, lastSummarizedMessageId);
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
                UUID lastSummarizedMessageId = messagesToSummarize.get(messagesToSummarize.size() - 1).getId();

                String updatedSummary = generalSummaryOfNoteBook(
                        noteBookEntity.getConversationSummary(),
                        messagesToSummarize);

                if (updatedSummary == null || updatedSummary.isBlank()) {
                    return;
                }

                noteBookService.updateConversationSummaryInternal(noteBookId, updatedSummary, lastSummarizedMessageId);
            } catch (JsonProcessingException | RuntimeException e) {
                log.error("Failed to generate conversation summary for notebook {}", noteBookId, e);
            }
        });
    }

    /**
     * Generate title for note based on content
     * 
     * @param input
     * @return
     * @throws JsonProcessingException
     */
    public String generalTitleOfNote(String input) throws JsonProcessingException {
        String prompt = "Act as a professional content editor. Your task is to generate a concise and descriptive title for a note based on the content provided below. "
                + "The title should accurately reflect the main topic or theme of the note while adhering to the following constraints:"
                + "### Constraints:"
                + "- Language: The title MUST be in the same language as the content."
                + "- Length: Maximum 6-10 words."
                + "- Format: Return ONLY the raw title text. Do not include quotes, punctuation at the end, or prefixes like \"Title:\"."
                + "- Tone: Professional and neutral."
                + "### Note Content: " + input + ""
                + "### Generated Title: ";

        RagCompletionRequestDto.Metadata metadata = new RagCompletionRequestDto.Metadata();
        metadata.setUserId(UUID.randomUUID());
        metadata.setOrganizationId(UUID.randomUUID());
        metadata.setScopes(Set.of(DataScope.PERSONAL.getKey().toLowerCase()));

        RagCompletionRequestDto ragCompletionRequestDto = applyAiSettings(RagCompletionRequestDto.builder()
                .messages(List.of(createRagMessage(MessageType.USER.getValue(), prompt)))
                .stream(false))
                .metadata(metadata)
                .build();

        return generateString(ragCompletionRequestDto);
    }

    /**
     * Generate title for topic based on user's input
     * 
     * @param input
     * @return
     * @throws JsonProcessingException
     */
    public String generalTitleOfTopic(String input) throws JsonProcessingException {
        String prompt = "Act as a professional content editor. Your task is to generate a concise and descriptive title for a chat conversation based on the user's initial input provided below. "
                + "The title should accurately reflect the main topic or theme of the conversation while adhering to the following constraints:"
                + "### Constraints:"
                + "- Language: The title MUST be in the same language as the user's input."
                + "- Length: Maximum 6-10 words."
                + "- Format: Return ONLY the raw title text. Do not include quotes, punctuation at the end, or prefixes like \"Title:\"."
                + "- Tone: Professional and neutral."
                + "### User Input: " + input + ""
                + "### Generated Title: ";

        RagCompletionRequestDto.Metadata metadata = new RagCompletionRequestDto.Metadata();
        metadata.setUserId(UUID.randomUUID());
        metadata.setOrganizationId(UUID.randomUUID());
        metadata.setScopes(Set.of(DataScope.PERSONAL.getKey().toLowerCase()));
        
        RagCompletionRequestDto ragCompletionRequestDto = applyAiSettings(RagCompletionRequestDto.builder()
                .messages(List.of(createRagMessage(MessageType.USER.getValue(), prompt)))
                .stream(false))
                .metadata(metadata)
                .build();

        return generateString(ragCompletionRequestDto);
    }

    public String generalSummaryOfTopic(String existingSummary, List<MessageResponseDto> messages)
            throws JsonProcessingException {
        StringBuilder prompt = new StringBuilder();
        prompt.append(
                "Act as a conversation memory compressor for Topic chat. Update a long-running topic summary so future turns retain important context. ");
        prompt.append(
                "Write in the same language as the conversation. Keep only durable facts, decisions, constraints, user preferences, named entities, unresolved questions, and progress state. ");
        prompt.append(
                "Do not include greetings, filler, duplicated wording, or markdown bullets unless they are essential. Return only the updated summary text.\n\n");
        prompt.append("Existing summary:\n");
        prompt.append(isBlank(existingSummary) ? "(none)" : existingSummary);
        prompt.append("\n\nNew messages to absorb:\n");

        for (MessageResponseDto message : messages) {
            prompt.append(message.getType()).append(": ").append(message.getContent()).append('\n');
        }

        prompt.append("\nUpdated summary:");

        RagCompletionRequestDto.Metadata metadata = new RagCompletionRequestDto.Metadata();
        metadata.setUserId(UUID.randomUUID());
        metadata.setOrganizationId(UUID.randomUUID());
        metadata.setScopes(Set.of(DataScope.PERSONAL.getKey().toLowerCase()));

        RagCompletionRequestDto ragCompletionRequestDto = applyAiSettings(RagCompletionRequestDto.builder()
                .messages(List.of(createRagMessage(MessageType.USER.getValue(), prompt.toString())))
                .stream(false))
                .metadata(metadata)
                .build();

        return generateString(ragCompletionRequestDto);
    }

    public String generalSummaryOfNoteBook(String existingSummary, List<MessageResponseDto> messages)
            throws JsonProcessingException {
        StringBuilder prompt = new StringBuilder();
        prompt.append(
                "Act as a conversation memory compressor for Notebook chat. Update the notebook conversation summary for long-term memory. ");
        prompt.append(
                "Write in the same language as the conversation. Prioritize: requirements, tasks, plans, assumptions, decisions, unresolved action items, and key references from exchanged content. ");
        prompt.append(
                "Do not include greetings, filler, duplicated wording, or markdown bullets unless essential. Return only the updated summary text.\n\n");
        prompt.append("Existing summary:\n");
        prompt.append(isBlank(existingSummary) ? "(none)" : existingSummary);
        prompt.append("\n\nNew messages to absorb:\n");

        for (MessageResponseDto message : messages) {
            prompt.append(message.getType()).append(": ").append(message.getContent()).append('\n');
        }

        prompt.append("\nUpdated summary:");

        RagCompletionRequestDto.Metadata metadata = new RagCompletionRequestDto.Metadata();
        metadata.setUserId(UUID.randomUUID());
        metadata.setOrganizationId(UUID.randomUUID());
        metadata.setScopes(Set.of(DataScope.PERSONAL.getKey().toLowerCase()));

        RagCompletionRequestDto ragCompletionRequestDto = applyAiSettings(RagCompletionRequestDto.builder()
                .messages(List.of(createRagMessage(MessageType.USER.getValue(), prompt.toString())))
                .stream(false))
                .metadata(metadata)
                .build();

        return generateString(ragCompletionRequestDto);
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

    /**
     * Áp dụng cấu hình AI từ system settings vào
     * {@link RagCompletionRequestDto.RagCompletionRequestDtoBuilder}.
     * Đọc các settings:
     * <ul>
     * <li>{@code ai.model} — model AI mặc định (ví dụ: gpt-4)</li>
     * <li>{@code ai.temperature} — nhiệt độ sinh (0.0 - 2.0)</li>
     * <li>{@code ai.maxTokens} — số token tối đa mỗi request</li>
     * </ul>
     * 
     * @param builder builder của RagCompletionRequestDto
     * @return builder đã được apply AI settings
     */
    private RagCompletionRequestDto.RagCompletionRequestDtoBuilder applyAiSettings(
            RagCompletionRequestDto.RagCompletionRequestDtoBuilder builder) {
        String model = systemSettingService.getString("ai.model", "");
        if (!isBlank(model)) {
            builder.model(model);
        }
        double temperature = systemSettingService.getDouble("ai.temperature", -1);
        if (temperature >= 0) {
            builder.temperature(temperature);
        }
        int maxTokens = systemSettingService.getInt("ai.maxTokens", -1);
        if (maxTokens > 0) {
            builder.maxTokens(maxTokens);
        }

        log.info("Applied AI settings: model={}, temperature={}, maxTokens={}", model, temperature, maxTokens);
        log.info("RagCompletionRequestDto builder: {}", builder);

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
