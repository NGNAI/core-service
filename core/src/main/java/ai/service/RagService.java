package ai.service;

import ai.dto.outer.rag.request.RagCompletionRequestDto;
import ai.dto.own.request.ConversationRequestDto;
import ai.dto.own.request.MessageCreateRequestDto;
import ai.dto.own.request.MessageUpdateRequestDto;
import ai.dto.own.request.TopicCreateRequestDto;
import ai.dto.own.request.filter.AttachmentFilterDto;
import ai.dto.own.request.filter.MessageFilterDto;
import ai.dto.own.response.AttachmentResponseDto;
import ai.dto.own.response.MessageResponseDto;
import ai.enums.MessageType;
import ai.enums.TopicType;
import ai.service.api.RagApiService;
import ai.util.JwtUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class RagService {
    RagApiService ragApiService;
    TopicService topicService;
    MessageService messageService;
    AttachmentService attachmentService;

    ObjectMapper objectMapper;

    public Flux<String> chat(UUID topicId, ConversationRequestDto requestDto) throws JsonProcessingException {
        //If topic not exists, create new topic
        if(topicId == null)
            topicId = topicService.create(TopicCreateRequestDto.builder()
                            .title(requestDto.getMessage())
                            .type(TopicType.PRIVATE.getValue())
                    .build()).getId();
        else
            topicService.validateTopicOfUser(topicId, JwtUtil.getUserId());

        UUID finalTopicId = topicId;

        topicService.validateTopicId(finalTopicId);

        //Insert user question
        messageService.create(
                MessageCreateRequestDto.builder()
                        .content(requestDto.getMessage())
                        .type(MessageType.USER.getValue())
                        .topicId(finalTopicId)
                        .build()
        );

        //Insert assistant question
        MessageResponseDto assistantMessage = messageService.create(
                MessageCreateRequestDto.builder()
                        .content("Answering")
                        .type(MessageType.ASSISTANT.getValue())
                        .topicId(finalTopicId)
                        .build()
        );

        MessageFilterDto messageFilterDto = new MessageFilterDto();
        messageFilterDto.setPageNumber(0);
        messageFilterDto.setPageSize(10);
        messageFilterDto.setSortBy("id");
        messageFilterDto.setSortDir("desc");

        //Query history
        List<RagCompletionRequestDto.Message> historyConversations = messageService.getAll(finalTopicId, messageFilterDto).getSecond()
                .stream().map(messageResponseDto -> RagCompletionRequestDto.Message.builder()
                        .role(messageResponseDto.getContent())
                        .content(messageResponseDto.getType()).build()).collect(Collectors.toList());

        Collections.reverse(historyConversations);


        // Get attachments of topic - Khoa xử lý tiếp nha
        AttachmentFilterDto attachmentFilterDto = new AttachmentFilterDto();
        attachmentFilterDto.setTopicId(finalTopicId);

        List<AttachmentResponseDto> attachments = attachmentService.getList(attachmentFilterDto).toList();

        RagCompletionRequestDto ragCompletionRequestDto = RagCompletionRequestDto.builder()
                .model("")
                .messages(List.of(RagCompletionRequestDto.Message.builder()
                        .role(MessageType.USER.getValue())
                        .content(requestDto.getMessage())
                        .build()))
                .history(historyConversations)
                .topK(10)
                .stream(true)
                .build();

        StringBuilder fullAnswer = new StringBuilder();
        StringBuilder source = new StringBuilder();
        return ragApiService.completions(ragCompletionRequestDto)
                .startWith(String.format("{\"messageId\": \"%s\"}",assistantMessage.getId()))
                .startWith(String.format("{\"topicId\": \"%s\"}",topicId))
                .doOnNext(raw -> {
                    try {
                        if(!raw.trim().equals("[DONE]")) {
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
                .doOnComplete(()->{
                    messageService.update(assistantMessage.getId(), MessageUpdateRequestDto.builder()
                                    .content(fullAnswer.toString())
                                    .source(source.toString())
                                    .topicId(finalTopicId)
                            .build());
                });
    }
}
