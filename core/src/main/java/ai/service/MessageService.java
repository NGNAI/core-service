package ai.service;

import ai.dto.own.request.MessageCreateRequestDto;
import ai.dto.own.request.MessageUpdateRequestDto;
import ai.dto.own.request.filter.MessageFilterDto;
import ai.dto.own.response.MessageResponseDto;
import ai.entity.postgres.MessageEntity;
import ai.entity.postgres.TopicEntity;
import ai.enums.ApiResponseStatus;
import ai.exeption.AppException;
import ai.mapper.MessageMapper;
import ai.model.CustomPairModel;
import ai.repository.MessageRepository;
import ai.util.JwtUtil;
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

    public MessageResponseDto update(UUID messageId, MessageUpdateRequestDto updateRequestDto){
        MessageEntity entity = messageRepository.findById(messageId).orElseThrow(()-> new AppException(ApiResponseStatus.MESSAGE_ID_NOT_EXISTS));
        messageMapper.updateEntity(entity,updateRequestDto);
        return messageMapper.entityToResponseDto(messageRepository.save(entity));
    }

//    public void delete(UUID id){
//        messageRepository.deleteById(id);
//    }
}
