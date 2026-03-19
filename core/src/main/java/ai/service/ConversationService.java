package ai.service;

import ai.dto.outer.rag.request.RagCompletionRequestDto;
import ai.dto.own.request.ConversationRequestDto;
import ai.dto.own.request.MessageCreateRequestDto;
import ai.dto.own.request.TopicCreateRequestDto;
import ai.dto.own.request.filter.MessageFilterDto;
import ai.enums.MessageType;
import ai.enums.TopicType;
import ai.service.api.RagApiService;
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
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class ConversationService {
    RagApiService ragApiService;
    TopicService topicService;
    MessageService messageService;

    ObjectMapper objectMapper;

    public Flux<String> chat(Integer topicId, ConversationRequestDto requestDto) throws JsonProcessingException {
        //If topic not exists, create new topic
        if(topicId == null)
            topicId = topicService.create(TopicCreateRequestDto.builder()
                            .title(requestDto.getMessage())
                            .type(TopicType.PRIVATE.getValue())
                    .build()).getId();

        int finalTopicId = topicId;

        topicService.validateTopicId(finalTopicId);

        MessageFilterDto messageFilterDto = new MessageFilterDto();
        messageFilterDto.setPageNumber(0);
        messageFilterDto.setPageSize(10);
        messageFilterDto.setSortBy("id");
        messageFilterDto.setSortDir("desc");

        //Query history
        List<RagCompletionRequestDto.Message> historyConversations = messageService.getAll(finalTopicId, messageFilterDto)
                .stream().map(messageResponseDto -> RagCompletionRequestDto.Message.builder()
                .role(messageResponseDto.getContent())
                .content(messageResponseDto.getType()).build()).collect(Collectors.toList());

        Collections.reverse(historyConversations);

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

        //Insert user question
        messageService.create(
                MessageCreateRequestDto.builder()
                        .content(requestDto.getMessage())
                        .type(MessageType.USER.getValue())
                        .topicId(finalTopicId)
                        .build()
        );

        StringBuilder fullAnswer = new StringBuilder();
        return ragApiService.completions(ragCompletionRequestDto)
                .startWith(String.format("{\"topicId:\": \"%d\"}",topicId))
                .doOnNext(raw -> {
                    try {
                        if(!raw.trim().equals("[DONE]")) {
                            JsonNode node = objectMapper.readTree(raw);

                            if (node.has("token")) {
                                fullAnswer.append(node.get("token").asText());
                            }
                        }
                    } catch (Exception e) {
                        log.error("Fail to parse stream token", e);
                    }
                })
                .doOnComplete(()->{
                    String finalAnswer = fullAnswer.toString();
                    messageService.create(
                            MessageCreateRequestDto.builder()
                                    .content(finalAnswer)
                                    .type(MessageType.ASSISTANT.getValue())
                                    .topicId(finalTopicId)
                            .build()
                    );
                });
    }
}
