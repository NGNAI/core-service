package ai.service;

import java.util.List;
import java.util.UUID;

import ai.entity.postgres.*;
import ai.enums.MessageParentType;
import ai.repository.TopicMessagesRepository;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.dto.own.request.MessageCreateRequestDto;
import ai.dto.own.request.MessageUpdateRequestDto;
import ai.dto.own.request.filter.MessageFilterDto;
import ai.dto.own.response.MessageResponseDto;
import ai.enums.ApiResponseStatus;
import ai.enums.MessageType;
import ai.exeption.AppException;
import ai.mapper.MessageMapper;
import ai.model.CustomPairModel;
import ai.repository.MessageRepository;
import ai.util.JwtUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class MessageService {
    TopicService topicService;
    NoteBookService noteBookService;

    MessageRepository messageRepository;
    TopicMessagesRepository topicMessagesRepository;
    MessageMapper messageMapper;
    ObjectMapper objectMapper;

    public CustomPairModel<Long,List<MessageResponseDto>> getAll(UUID parentId,MessageParentType parentType, MessageFilterDto filterDto){
        Page<TopicMessageEntity> page = null;

        switch (parentType) {
            case TOPIC -> {
                topicService.validateTopicOfUser(parentId, JwtUtil.getUserId());
                Specification<TopicMessageEntity> spec = (root, query, criteriaBuilder) -> {
                    Fetch<TopicMessageEntity, MessageEntity> messageFetch = root.fetch("message", JoinType.INNER);
                    Join<TopicMessageEntity, MessageEntity> messageJoin = (Join<TopicMessageEntity, MessageEntity>) messageFetch;

                    Predicate messageSearch = filterDto.createSpec(messageJoin, criteriaBuilder);
                    Predicate orgIdSearch = criteriaBuilder.equal(root.get("topic").get("id"), parentId);
                    return criteriaBuilder.and(messageSearch, orgIdSearch);
                };
                filterDto.setSortPrefix("message");
                page = topicMessagesRepository.findAll(spec,filterDto.createPageable());
            }
            case NOTEBOOK -> {
//                noteBookService.validateTopicOfUser(createRequestDto.getParentId(), JwtUtil.getUserId());
//                Note topicEntity = topicService.getEntityById(createRequestDto.getParentId());
//                topicMessagesRepository.save(new TopicMessageEntity(topicEntity,newEntity));
            }
        }
        if(page == null)
            throw new AppException(ApiResponseStatus.UNEXPECTED);

        List<MessageResponseDto> messages = page.getContent().stream().map(entity -> {
            MessageResponseDto responseDto = messageMapper.entityToResponseDto(entity.getMessage());
            responseDto.setParentId(parentId);
            responseDto.setParentType(parentType.getValue());
            return responseDto;
        }).toList();

        return new CustomPairModel<>(page.getTotalElements(),messages);
    }

    public MessageResponseDto create(UUID parentId, MessageParentType parentType, MessageCreateRequestDto createRequestDto){
        MessageEntity newEntity = messageRepository.save(messageMapper.createRequestDtoToEntity(createRequestDto));

        switch (parentType) {
            case TOPIC -> {
                topicService.validateTopicOfUser(parentId, JwtUtil.getUserId());
                TopicEntity topicEntity = topicService.getEntityById(parentId);
                topicMessagesRepository.save(new TopicMessageEntity(topicEntity,newEntity));
            }
            case NOTEBOOK -> {
//                noteBookService.validateTopicOfUser(createRequestDto.getParentId(), JwtUtil.getUserId());
//                Note topicEntity = topicService.getEntityById(createRequestDto.getParentId());
//                topicMessagesRepository.save(new TopicMessageEntity(topicEntity,newEntity));
            }
        }
        MessageResponseDto responseDto = messageMapper.entityToResponseDto(newEntity);
        responseDto.setParentId(parentId);
        responseDto.setParentType(parentType.getValue());

        return responseDto;
    }

    /**
     * Tạo message kiểu attachment, dùng để lưu thông tin file khi user upload file lên topic. Sau đó message này sẽ được gửi vào rag để rag lưu thông tin file vào vector db cùng với nội dung message (nếu có) để phục vụ cho việc tìm kiếm sau này
     * @param topicId
     * @param source
     * @return
     */
    public MessageResponseDto createAttachmentMessage(UUID topicId, Object source) {
        String content;
        try {
            content = objectMapper.writeValueAsString(source);
        } catch (Exception e) {
            content = "{}";
        }
        return create(
                topicId,
                MessageParentType.TOPIC,
                MessageCreateRequestDto.builder()
                .type(MessageType.FILE.getValue())
                .content(content)
                .build());
    }

    public MessageResponseDto update(UUID messageId, MessageUpdateRequestDto updateRequestDto){
        MessageEntity entity = messageRepository.findById(messageId).orElseThrow(()-> new AppException(ApiResponseStatus.MESSAGE_ID_NOT_EXISTS));
        messageMapper.updateEntity(entity,updateRequestDto);
        return messageMapper.entityToResponseDto(messageRepository.save(entity));
    }
}
