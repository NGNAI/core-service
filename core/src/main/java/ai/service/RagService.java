package ai.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

    AppProperties appProperties;
    RagApiService ragApiService;
    TopicService topicService;
    NoteBookService noteBookService;
    DraftService draftService;
    MessageService messageService;
    SystemEventSseService systemEventSseService;

    ObjectMapper objectMapper;

    /**
     * Chat with topic, if topicId is null, create new topic and chat, else chat
     * with exist topic
     * 
     * @param topicId
     * @param requestDto
     * @return
     * @throws JsonProcessingException
     */
    public Flux<String> chatTopic(UUID topicId, TopicCreateConversationRequestDto requestDto, List<TopicSourceResponseDto> uploadedSources)
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
            .stream().map(messageResponseDto -> createRagMessage(messageResponseDto.getType(), messageResponseDto.getContent()))
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
        //List<TopicSourceResponseDto> attachments = topicSourceService.getAllSources(finalTopicId);

        RagCompletionRequestDto.Metadata metadata = new RagCompletionRequestDto.Metadata();
        metadata.setUserId(JwtUtil.getUserId());
        metadata.setOrganizationId(JwtUtil.getOrgId());
        metadata.setTopic_id(finalTopicId);
        metadata.setScopes(requestDto.getScopes());
        //metadata.setFileIds(attachments.stream().map(e -> e.getId().toString()).collect(Collectors.toSet()));
        // Chỗ fileIds này tạm thời là lấy theo attachment của message đầu vào, sau này có thể điều chỉnh lại nếu muốn lấy attachment theo topic thay vì message (hiện tại FE chưa support upload attachment riêng cho message, mà chỉ có upload attachment chung cho topic, nên tạm thời cứ lấy attachment của message đầu vào đã, sau này nếu FE support upload attachment riêng cho message thì sẽ lấy attachment theo message thay vì topic)
        metadata.setFileIds(uploadedSources != null
                ? uploadedSources.stream().map(e -> e.getId().toString()).collect(Collectors.toSet())
                : Collections.emptySet());
        metadata.setSummaries(buildSummaryMetadata(topicEntity));

        RagCompletionRequestDto ragCompletionRequestDto = RagCompletionRequestDto.builder()
            .messages(historyConversations)
            .metadata(metadata)
            .stream(true)
            .build();

        StringBuilder fullAnswer = new StringBuilder();
        StringBuilder source = new StringBuilder();

        System.out.println(new ObjectMapper().writeValueAsString(ragCompletionRequestDto));

        return ragApiService.topicChat(ragCompletionRequestDto)
                .startWith(String.format("{\"messageId\": \"%s\"}", assistantMessage.getId()))
                .startWith(String.format("{\"topicId\": \"%s\"}", topicId))
                // Trả thêm về dto assistant message luôn
                .startWith(String.format("{\"assistantMessage\": %s}", new ObjectMapper().writeValueAsString(assistantMessage)))
                .doOnNext(raw -> {
                    try {
                        if (!raw.trim().equals("[DONE]")) {
                            JsonNode node = objectMapper.readTree(raw);

                            if (node.has("token")) {
                                fullAnswer.append(node.get("token").asText());
                            }
                            if (node.has("sources")) {
                                source.append(node.get("sources"));
                            }
                        }
                    } catch (JsonProcessingException e) {
                        log.error("Fail to parse stream token", e);
                    }
                })
                .doOnComplete(() -> {
                    if(source.isEmpty()) {
                        source.append("[]");
                    }
                    messageService.update(assistantMessage.getId(), MessageUpdateRequestDto.builder()
                            .content(fullAnswer.toString())
                            .source(source.toString())
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
            .stream().map(messageResponseDto -> createRagMessage(messageResponseDto.getType(), messageResponseDto.getContent()))
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

        RagCompletionRequestDto ragCompletionRequestDto = RagCompletionRequestDto.builder()
            .messages(historyConversations)
            .metadata(metadata)
            .stream(true)
            .build();

        StringBuilder fullAnswer = new StringBuilder();
        StringBuilder source = new StringBuilder();

        System.out.println(new ObjectMapper().writeValueAsString(ragCompletionRequestDto));

        return ragApiService.noteBookChat(ragCompletionRequestDto)
                .startWith(String.format("{\"messageId\": \"%s\"}", assistantMessage.getId()))
                .startWith(String.format("{\"noteBookId\": \"%s\"}", noteBookId))
                // Trả thêm về dto assistant message luôn
                .startWith(String.format("{\"assistantMessage\": %s}", new ObjectMapper().writeValueAsString(assistantMessage)))
                .doOnNext(raw -> {
                    try {
                        if (!raw.trim().equals("[DONE]")) {
                            JsonNode node = objectMapper.readTree(raw);

                            if (node.has("token")) {
                                fullAnswer.append(node.get("token").asText());
                            }
                            if (node.has("sources")) {
                                source.append(node.get("sources").asText());
                            }
                        }
                    } catch (JsonProcessingException e) {
                        log.error("Fail to parse stream token", e);
                    }
                })
                .doOnComplete(() -> {
                    if(source.isEmpty()) {
                        source.append("[]");
                    }
                    messageService.update(assistantMessage.getId(), MessageUpdateRequestDto.builder()
                            .content(fullAnswer.toString())
                            .source(source.toString())
                            .build());

                    asyncUpdateNoteBookSummary(finalNoteBookId);
                });
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
            .context(draftResponse.getDetailedDescription())
            .userId(capturedUserId)
            .organizationId(capturedOrgId)
            .stream(true)
            .build();

        StringBuilder sessionId = new StringBuilder();
        StringBuilder status = new StringBuilder();
        StringBuilder questionForUser = new StringBuilder();
        StringBuilder draftContent = new StringBuilder();
        StringBuilder thoughts = new StringBuilder();

        return ragApiService.draftCreate(ragDraftCreateRequestDto)
                .startWith(String.format("{\"messageId\": \"%s\"}", assistantMessage.getId()))
                .startWith(String.format("{\"draftId\": \"%s\"}", draftResponse.getId()))
                .startWith(String.format("{\"assistantMessage\": %s}", new ObjectMapper().writeValueAsString(assistantMessage)))
                .doOnNext(raw -> {
                    try {
                        JsonNode node = objectMapper.readTree(raw);
                        if (node.has("type") && node.get("type").asText().equalsIgnoreCase("done")) {
                            JsonNode response = node.get("response");
                            if (response != null) {
                                if (response.has("session_id")) {
                                    sessionId.append(response.get("session_id").asText());
                                }

                                if (response.has("status")) {
                                    status.append(response.get("status").asText());
                                }

                                if (response.has("question_for_user")) {
                                    questionForUser.append(response.get("question_for_user").asText());
                                }

                                if (response.has("draft")) {
                                    draftContent.append(response.get("draft").asText());
                                }

                                if (response.has("thoughts")) {
                                    thoughts.append(response.get("thoughts").asText());
                                }
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
                    if(!status.isEmpty() && status.toString().equalsIgnoreCase("completed")) {
                        questionForUser.setLength(0);
                        questionForUser.append("Đã hoàn thành");
                    }

                    if(thoughts.isEmpty()) {
                        thoughts.append("[]");
                    }
                    messageService.update(assistantMessage.getId(), MessageUpdateRequestDto.builder()
                            .content(questionForUser.toString())
                            .source(thoughts.toString())
                            .build());

                    // Lưu thành 1 version mới của draft để theo dõi lịch sử chỉnh sửa nếu có draftContent
                    String draftContentStr = draftContent.toString();
                    if(!draftContentStr.isEmpty()) {
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
                        assistantMessage.setSource(thoughts.toString());
                        log.info("assistantMessage before sending: {}", assistantMessage);
                        return String.format(
                            "{\"updatedAssistantMessage\": %s}",
                            new ObjectMapper().writeValueAsString(assistantMessage)
                        );
                    }).flatMapMany(Flux::just)
                )
                .doOnError(e -> {
                    log.error("Error during draft chat streaming", e);
                })
                .doFinally(signalType -> log.info("Draft chat streaming completed with signal: {}", signalType));
    }

    /**
     * Chat with draft, draft is a special type that only user can see, used for user to iteratively edit a piece of content via chatting with AI. If draftId is null, create new draft and chat, else chat with exist draft
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
            .build();

        StringBuilder status = new StringBuilder();
        StringBuilder questionForUser = new StringBuilder();
        StringBuilder draftContent = new StringBuilder();
        StringBuilder thoughts = new StringBuilder();

        return ragApiService.draftRevise(ragDraftReviseRequestDto)
                .startWith(String.format("{\"messageId\": \"%s\"}", assistantMessage.getId()))
                .startWith(String.format("{\"draftId\": \"%s\"}", draftId))
                .startWith(String.format("{\"assistantMessage\": %s}", new ObjectMapper().writeValueAsString(assistantMessage)))
                .doOnNext(raw -> {
                    try {
                        JsonNode node = objectMapper.readTree(raw);
                        if (node.has("type") && node.get("type").asText().equalsIgnoreCase("done")) {
                            JsonNode response = node.get("response");
                            if (response != null) {
                                if (response.has("status")) {
                                    status.append(response.get("status").asText());
                                }

                                if (response.has("question_for_user")) {
                                    questionForUser.append(response.get("question_for_user").asText());
                                }

                                if (response.has("draft")) {
                                    draftContent.append(response.get("draft").asText());
                                }

                                if (response.has("thoughts")) {
                                    thoughts.append(response.get("thoughts"));
                                }
                            }
                        }
                    } catch (JsonProcessingException e) {
                        log.error("Fail to parse stream token", e);
                    }
                })
                .doOnComplete(() -> {
                    // Update assistant message with generated content
                    if(!status.isEmpty() && status.toString().equalsIgnoreCase("completed")) {
                        questionForUser.setLength(0);
                        questionForUser.append("Đã hoàn thành");
                    }

                    if(thoughts.isEmpty()) {
                        thoughts.append("[]");
                    } 
                    messageService.update(assistantMessage.getId(), MessageUpdateRequestDto.builder()
                            .content(questionForUser.toString())
                            .source(thoughts.toString())
                            .build());

                    // Lưu thành 1 version mới của draft để theo dõi lịch sử chỉnh sửa nếu có draftContent
                    String draftContentStr = draftContent.toString();
                    if(!draftContentStr.isEmpty()) {
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
                        assistantMessage.setSource(thoughts.toString());
                        log.info("assistantMessage before sending: {}", assistantMessage);
                        return String.format(
                            "{\"updatedAssistantMessage\": %s}",
                            new ObjectMapper().writeValueAsString(assistantMessage)
                        );
                    }).flatMapMany(Flux::just)
                )
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

        RagCompletionRequestDto ragCompletionRequestDto = RagCompletionRequestDto.builder()
            .messages(List.of(createRagMessage(MessageType.USER.getValue(), prompt)))
            .stream(false)
            .build();

        return ragApiService.general(ragCompletionRequestDto);
    }

    public String generalSummaryOfTopic(String existingSummary, List<MessageResponseDto> messages) throws JsonProcessingException {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Act as a conversation memory compressor for Topic chat. Update a long-running topic summary so future turns retain important context. ");
        prompt.append("Write in the same language as the conversation. Keep only durable facts, decisions, constraints, user preferences, named entities, unresolved questions, and progress state. ");
        prompt.append("Do not include greetings, filler, duplicated wording, or markdown bullets unless they are essential. Return only the updated summary text.\n\n");
        prompt.append("Existing summary:\n");
        prompt.append(isBlank(existingSummary) ? "(none)" : existingSummary);
        prompt.append("\n\nNew messages to absorb:\n");

        for (MessageResponseDto message : messages) {
            prompt.append(message.getType()).append(": ").append(message.getContent()).append('\n');
        }

        prompt.append("\nUpdated summary:");

        RagCompletionRequestDto ragCompletionRequestDto = RagCompletionRequestDto.builder()
            .messages(List.of(createRagMessage(MessageType.USER.getValue(), prompt.toString())))
            .stream(false)
            .build();

        return ragApiService.general(ragCompletionRequestDto);
    }

    public String generalSummaryOfNoteBook(String existingSummary, List<MessageResponseDto> messages) throws JsonProcessingException {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Act as a conversation memory compressor for Notebook chat. Update the notebook conversation summary for long-term memory. ");
        prompt.append("Write in the same language as the conversation. Prioritize: requirements, tasks, plans, assumptions, decisions, unresolved action items, and key references from exchanged content. ");
        prompt.append("Do not include greetings, filler, duplicated wording, or markdown bullets unless essential. Return only the updated summary text.\n\n");
        prompt.append("Existing summary:\n");
        prompt.append(isBlank(existingSummary) ? "(none)" : existingSummary);
        prompt.append("\n\nNew messages to absorb:\n");

        for (MessageResponseDto message : messages) {
            prompt.append(message.getType()).append(": ").append(message.getContent()).append('\n');
        }

        prompt.append("\nUpdated summary:");

        RagCompletionRequestDto ragCompletionRequestDto = RagCompletionRequestDto.builder()
                .messages(List.of(createRagMessage(MessageType.USER.getValue(), prompt.toString())))
                .stream(false)
                .build();

        return ragApiService.general(ragCompletionRequestDto);
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
     * Định nghĩa khi nào cần gọi API tóm tắt lại cuộc hội thoại để cập nhật summary. Nếu tổng số tin nhắn sau checkpoint (tức là tin nhắn chưa được tóm tắt) vượt quá recentWindow + minMessagesToCompress, thì sẽ gọi API tóm tắt.
     * recentWindow đảm bảo rằng luôn có một số lượng tin nhắn gần đây được giữ nguyên trong summary để duy trì ngữ cảnh tươi mới, trong khi minMessagesToCompress đảm bảo rằng chỉ gọi API tóm tắt khi có đủ tin nhắn mới cần được nén lại, tránh việc gọi API quá thường xuyên với lượng tin nhắn quá ít.
     * @param totalMessagesAfterCheckpoint tổng số tin nhắn sau checkpoint
     * @param recentWindow số lượng tin nhắn gần đây được giữ nguyên trong summary
     * @return true nếu cần tóm tắt, false nếu không
     */
    private boolean shouldSummarize(int totalMessagesAfterCheckpoint, int recentWindow) {
        return totalMessagesAfterCheckpoint > recentWindow + minMessagesToCompress();
    }

    /**
     * Đọc cấu hình từ appProperties, nếu không có hoặc không hợp lệ (null hoặc <=0) thì trả về giá trị mặc định.
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

}
