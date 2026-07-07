package ai.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import ai.annotation.Audited;
import ai.dto.own.request.TopicCreateRequestDto;
import ai.dto.own.request.TopicRenameTitleRequestDto;
import ai.dto.own.request.filter.TopicFilterDto;
import ai.dto.own.response.TopicResponseDto;
import ai.entity.postgres.TopicEntity;
import ai.enums.ApiResponseStatus;
import ai.enums.AuditAction;
import ai.enums.AuditResource;
import ai.exeption.AppException;
import ai.mapper.TopicMapper;
import ai.model.CustomPairModel;
import ai.repository.TopicRepository;
import ai.util.JwtUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class TopicService {
    TopicRepository topicRepository;
    UserService userService;
    OrganizationService organizationService;

    TopicMapper topicMapper;

    public void validateTopicOfUser(UUID topicId, UUID userId){
        if(!topicRepository.existsByIdAndOwnerId(topicId,userId))
            throw new AppException(ApiResponseStatus.PERMISSION_DENIED);
    }

    public void validateTopicId(UUID topicId){
        if(!topicRepository.existsById(topicId))
            throw new AppException(ApiResponseStatus.TOPIC_ID_NOT_EXISTS);
    }

    public TopicEntity getEntityById(UUID topicId){
        return topicRepository.findById(topicId).orElseThrow(() -> new AppException(ApiResponseStatus.TOPIC_ID_NOT_EXISTS));
    }

    public CustomPairModel<Long,List<TopicResponseDto>> getAll(TopicFilterDto filterDto){
        UUID userId = JwtUtil.getUserId();
        UUID organizationId = JwtUtil.getOrgId();

        userService.validateUserId(userId);
        organizationService.validateOrgId(organizationId);

        Specification<TopicEntity> spec = filterDto.createSpec().and((root, query, criteriaBuilder) -> {
            Predicate orgIdPredicate = criteriaBuilder.equal(root.get("organization").get("id"), organizationId);
            Predicate ownerPredicate = criteriaBuilder.equal(root.get("owner").get("id"), userId);
            return criteriaBuilder.and(orgIdPredicate, ownerPredicate);
        });

        Page<TopicEntity> page = topicRepository.findAll(
                spec,
                filterDto.createPageable()
        );

        return new CustomPairModel<>(page.getTotalElements(),page.getContent().stream().map(topicMapper::entityToResponseDto).toList());
    }

    @Audited(action = AuditAction.CREATE, resource = AuditResource.TOPIC, description = "Tạo chủ đề: {0}")
    public TopicResponseDto create(TopicCreateRequestDto createRequestDto){
        TopicEntity newEntity = topicMapper.createRequestDtoToEntity(createRequestDto);
        newEntity.setOwner(userService.getEntityById(JwtUtil.getUserId()));
        newEntity.setOrganization(organizationService.getEntityById(JwtUtil.getOrgId()));

        return topicMapper.entityToResponseDto(topicRepository.save(newEntity));
    }

    @Audited(action = AuditAction.UPDATE, resource = AuditResource.TOPIC, resourceIdExpression = "#arg0", description = "Đổi tên chủ đề: {0}")
    public TopicResponseDto renameTitle(UUID id, TopicRenameTitleRequestDto requestDto){
        validateTopicOfUser(id,JwtUtil.getUserId());
        TopicEntity entity = topicRepository.findById(id).orElseThrow(() -> new AppException(ApiResponseStatus.TOPIC_ID_NOT_EXISTS));
        entity.setTitle(requestDto.getTitle());

        return topicMapper.entityToResponseDto(topicRepository.save(entity));
    }

    @Audited(action = AuditAction.DELETE, resource = AuditResource.TOPIC, resourceIdExpression = "#arg0", description = "Xoá chủ đề: {0}")
    public void delete(UUID id){
        validateTopicOfUser(id,JwtUtil.getUserId());
        topicRepository.deleteById(id);
    }

    /**
     * Update the title of a topic with the given ID.
     * @param topicId
     * @param title
     */
    public void updateTitleInternal(UUID topicId, String title) {
        topicRepository.findById(topicId).ifPresent(entity -> {
            entity.setTitle(title);
            topicRepository.save(entity);
        });
    }

    /**
     * Update the conversation summary and the last message ID for a given topic.
     * @param topicId
     * @param summary
     * @param lastMessageId
     */
    public void updateConversationSummaryInternal(UUID topicId, String summary, UUID lastMessageId) {
        topicRepository.findById(topicId).ifPresent(entity -> {
            entity.setConversationSummary(summary);
            entity.setConversationSummaryLastMessageId(lastMessageId);
            topicRepository.save(entity);
        });
    }
}
