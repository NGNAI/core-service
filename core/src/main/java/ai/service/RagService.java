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
import ai.dto.own.request.MessageCreateRequestDto;
import ai.dto.own.request.MessageUpdateRequestDto;
import ai.dto.own.request.NoteBookCreateConversationRequestDto;
import ai.dto.own.request.NoteBookCreateRequestDto;
import ai.dto.own.request.TopicCreateConversationRequestDto;
import ai.dto.own.request.TopicCreateRequestDto;
import ai.dto.own.request.filter.MessageFilterDto;
import ai.dto.own.response.MessageResponseDto;
import ai.dto.own.response.TopicSourceResponseDto;
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
    TopicSourceService topicSourceService;
    NoteBookService noteBookService;
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
    public Flux<String> chatTopic(UUID topicId, TopicCreateConversationRequestDto requestDto)
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
        List<TopicSourceResponseDto> attachments = topicSourceService.getAllSources(finalTopicId);

        RagCompletionRequestDto.Metadata metadata = new RagCompletionRequestDto.Metadata();
        metadata.setUserId(JwtUtil.getUserId());
        metadata.setOrganizationId(JwtUtil.getOrgId());
        metadata.setScopes(requestDto.getScopes());
        metadata.setFileIds(attachments.stream().map(e -> e.getId().toString()).collect(Collectors.toSet()));
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
                    } catch (Exception e) {
                        log.error("Fail to parse stream token", e);
                    }
                })
                .doOnComplete(() -> {
                    messageService.update(assistantMessage.getId(), MessageUpdateRequestDto.builder()
                            .content(fullAnswer.toString())
                            .source(source.toString())
                            .build());

                    asyncUpdateTopicSummary(finalTopicId);
                });
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
        metadata.setFileIds(requestDto.getSourceIds());
        metadata.setSummaries(buildSummaryMetadata(noteBookEntity));

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
                    } catch (Exception e) {
                        log.error("Fail to parse stream token", e);
                    }
                })
                .doOnComplete(() -> {
                    messageService.update(assistantMessage.getId(), MessageUpdateRequestDto.builder()
                            .content(fullAnswer.toString())
                            .source(source.toString())
                            .build());

                    asyncUpdateNoteBookSummary(finalNoteBookId);
                });
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
            } catch (Exception e) {
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
            } catch (Exception e) {
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
            } catch (Exception e) {
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

    private boolean shouldSummarize(int totalMessagesAfterCheckpoint, int recentWindow) {
        return totalMessagesAfterCheckpoint > recentWindow + minMessagesToCompress();
    }

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
