package ai.service;

import ai.dto.own.request.MessageCreateRequestDto;
import ai.dto.own.request.filter.MessageFilterDto;
import ai.dto.own.response.MessageResponseDto;
import ai.entity.postgres.MessageEntity;
import ai.entity.postgres.TopicEntity;
import ai.mapper.MessageMapper;
import ai.model.CustomPairModel;
import ai.repository.MessageRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class MessageService {
    MessageRepository messageRepository;
    TopicService topicService;

    MessageMapper messageMapper;

    public CustomPairModel<Long,List<MessageResponseDto>> getAll(int topicId, MessageFilterDto filterDto){
        topicService.validateTopicId(topicId);

        Specification<MessageEntity> spec = filterDto.createSpec().and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("topic").get("id"),topicId));

        Page<MessageEntity> page = messageRepository.findAll(spec,filterDto.createPageable());

        return new CustomPairModel<>(page.getTotalElements(),page.getContent().stream().map(messageMapper::entityToResponseDto).toList());
    }

    public MessageResponseDto create(MessageCreateRequestDto createRequestDto){
        TopicEntity topicEntity = topicService.getEntityById(createRequestDto.getTopicId());
        MessageEntity newEntity = messageMapper.createRequestDtoToEntity(createRequestDto);
        newEntity.setTopic(topicEntity);

        return messageMapper.entityToResponseDto(messageRepository.save(newEntity));
    }

    public void delete(int id){
        messageRepository.deleteById(id);
    }
}
