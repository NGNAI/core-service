package ai.service;

import ai.dto.own.request.MessageCreateRequestDto;
import ai.dto.own.request.MessageUpdateRequestDto;
import ai.dto.own.request.filter.MessageFilterDto;
import ai.dto.own.response.AttachmentResponseDto;
import ai.dto.own.response.MessageResponseDto;
import ai.entity.postgres.MessageEntity;
import ai.entity.postgres.TopicEntity;
import ai.enums.ApiResponseStatus;
import ai.enums.MessageType;
import ai.exeption.AppException;
import ai.mapper.MessageMapper;
import ai.model.CustomPairModel;
import ai.repository.MessageRepository;
import ai.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class MessageService {
    MessageRepository messageRepository;
    TopicService topicService;
    MessageMapper messageMapper;
    ObjectMapper objectMapper;

    public CustomPairModel<Long,List<MessageResponseDto>> getAll(UUID topicId, MessageFilterDto filterDto){
        topicService.validateTopicOfUser(topicId, JwtUtil.getUserId());

        Specification<MessageEntity> spec = filterDto.createSpec().and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("topic").get("id"),topicId));

        Page<MessageEntity> page = messageRepository.findAll(spec,filterDto.createPageable());

        List<MessageResponseDto> messages = page.getContent().stream().map(entity -> {
            MessageResponseDto responseDto = messageMapper.entityToResponseDto(entity);
            responseDto.setTopicId(topicId);
            return responseDto;
        }).toList();

        return new CustomPairModel<>(page.getTotalElements(),messages);
    }

    public MessageResponseDto create(MessageCreateRequestDto createRequestDto){
        topicService.validateTopicOfUser(createRequestDto.getTopicId(), JwtUtil.getUserId());
        TopicEntity topicEntity = topicService.getEntityById(createRequestDto.getTopicId());
        MessageEntity newEntity = messageMapper.createRequestDtoToEntity(createRequestDto);
        newEntity.setTopic(topicEntity);

        return messageMapper.entityToResponseDto(messageRepository.save(newEntity));
    }

    /**
     * Tạo message kiểu attachment, dùng để lưu thông tin file khi user upload file lên topic. Sau đó message này sẽ được gửi vào rag để rag lưu thông tin file vào vector db cùng với nội dung message (nếu có) để phục vụ cho việc tìm kiếm sau này
     * @param topicId
     * @param attachmentDto
     * @return
     */
    public MessageResponseDto createAttachmentMessage(UUID topicId, AttachmentResponseDto attachmentDto) {
        return createAttachmentMessage(topicId, (Object) attachmentDto);
    }

    public MessageResponseDto createAttachmentMessage(UUID topicId, Object attachmentDto) {
        String content;
        try {
            content = objectMapper.writeValueAsString(attachmentDto);
        } catch (Exception e) {
            content = "{}";
        }
        return create(MessageCreateRequestDto.builder()
                .topicId(topicId)
                .type(MessageType.FILE.getValue())
                .content(content)
                .build());
    }

    public MessageResponseDto update(UUID messageId, MessageUpdateRequestDto updateRequestDto){
        MessageEntity entity = messageRepository.findById(messageId).orElseThrow(()-> new AppException(ApiResponseStatus.MESSAGE_ID_NOT_EXISTS));
        messageMapper.updateEntity(entity,updateRequestDto);
        return messageMapper.entityToResponseDto(messageRepository.save(entity));
    }

//    public void delete(UUID id){
//        messageRepository.deleteById(id);
//    }
}
