package ai.service;

import ai.dto.own.request.TopicCreateRequestDto;
import ai.dto.own.request.TopicRenameTitleRequestDto;
import ai.dto.own.request.filter.TopicFilterDto;
import ai.dto.own.response.TopicResponseDto;
import ai.entity.postgres.MessageEntity;
import ai.entity.postgres.TopicEntity;
import ai.enums.ApiResponseStatus;
import ai.exeption.AppException;
import ai.mapper.TopicMapper;
import ai.repository.TopicRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class TopicService {
    TopicRepository topicRepository;
    UserService userService;

    TopicMapper topicMapper;

    public void validateTopicId(int topicId){
        if(!topicRepository.existsById(topicId))
            throw new AppException(ApiResponseStatus.TOPIC_ID_NOT_EXISTS);
    }

    public TopicEntity getEntityById(int topicId){
        return topicRepository.findById(topicId).orElseThrow(() -> new AppException(ApiResponseStatus.TOPIC_ID_NOT_EXISTS));
    }

    public List<TopicResponseDto> getAll(int userId, TopicFilterDto filterDto){
        userService.validateUserId(userId);
        Specification<TopicEntity> spec = filterDto.createSpec().and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("owner").get("id"), userId));

        return topicRepository.findAll(
                spec,
                filterDto.createPageable()
        ).stream().map(topicMapper::entityToResponseDto).toList();
    }

    public TopicResponseDto create(TopicCreateRequestDto createRequestDto){
        TopicEntity newEntity = topicMapper.createRequestDtoToEntity(createRequestDto);
        newEntity.setUser(userService.getEntityById(1));

        return topicMapper.entityToResponseDto(topicRepository.save(newEntity));
    }

    public TopicResponseDto renameTitle(int id, TopicRenameTitleRequestDto requestDto){
        TopicEntity entity = topicRepository.findById(id).orElseThrow(() -> new AppException(ApiResponseStatus.TOPIC_ID_NOT_EXISTS));

        entity.setTitle(requestDto.getTitle());

        return topicMapper.entityToResponseDto(topicRepository.save(entity));
    }

    public void delete(int id){
        topicRepository.deleteById(id);
    }
}
