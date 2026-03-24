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
import ai.model.CustomPairModel;
import ai.repository.TopicRepository;
import ai.util.JwtUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
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

    public void validateTopicId(UUID topicId){
        if(!topicRepository.existsById(topicId))
            throw new AppException(ApiResponseStatus.TOPIC_ID_NOT_EXISTS);
    }

    public TopicEntity getEntityById(UUID topicId){
        return topicRepository.findById(topicId).orElseThrow(() -> new AppException(ApiResponseStatus.TOPIC_ID_NOT_EXISTS));
    }

    public CustomPairModel<Long,List<TopicResponseDto>> getAll(TopicFilterDto filterDto){
        UUID userId = JwtUtil.getUserId();
        userService.validateUserId(userId);
        Specification<TopicEntity> spec = filterDto.createSpec().and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("owner").get("id"), userId));

        Page<TopicEntity> page = topicRepository.findAll(
                spec,
                filterDto.createPageable()
        );

        return new CustomPairModel<>(page.getTotalElements(),page.getContent().stream().map(topicMapper::entityToResponseDto).toList());
    }

    public TopicResponseDto create(TopicCreateRequestDto createRequestDto){
        TopicEntity newEntity = topicMapper.createRequestDtoToEntity(createRequestDto);
        newEntity.setOwner(userService.getEntityById(JwtUtil.getUserId()));

        return topicMapper.entityToResponseDto(topicRepository.save(newEntity));
    }

    public TopicResponseDto renameTitle(UUID id, TopicRenameTitleRequestDto requestDto){
        TopicEntity entity = topicRepository.findById(id).orElseThrow(() -> new AppException(ApiResponseStatus.TOPIC_ID_NOT_EXISTS));
        entity.setTitle(requestDto.getTitle());

        return topicMapper.entityToResponseDto(topicRepository.save(entity));
    }

    public void delete(UUID id){
        topicRepository.deleteById(id);
    }
}
