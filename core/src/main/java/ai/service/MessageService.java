package ai.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.dto.own.request.MessageCreateRequestDto;
import ai.dto.own.request.MessageUpdateRequestDto;
import ai.dto.own.request.filter.MessageFilterDto;
import ai.dto.own.response.MessageFeedbackHistoryResponseDto;
import ai.dto.own.response.MessageResponseDto;
import ai.entity.postgres.MessageEntity;
import ai.entity.postgres.MessageFeedbackHistoryEntity;
import ai.entity.postgres.NoteBookEntity;
import ai.entity.postgres.NotebookMessageEntity;
import ai.entity.postgres.TopicEntity;
import ai.entity.postgres.TopicMessageEntity;
import ai.enums.ApiResponseStatus;
import ai.enums.MessageFeedbackType;
import ai.enums.MessageParentType;
import ai.enums.MessageType;
import ai.exeption.AppException;
import ai.interfaces.MessageRelationEntity;
import ai.mapper.MessageMapper;
import ai.model.CustomPairModel;
import ai.repository.MessageFeedbackHistoryRepository;
import ai.repository.MessageRepository;
import ai.repository.NotebookMessagesRepository;
import ai.repository.TopicMessagesRepository;
import ai.util.JwtUtil;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class MessageService {
    static final int DEFAULT_HISTORY_PAGE_SIZE = 20;
    static final int MAX_HISTORY_PAGE_SIZE = 100;

    TopicService topicService;
    NoteBookService noteBookService;

    MessageRepository messageRepository;
    MessageFeedbackHistoryRepository messageFeedbackHistoryRepository;
    TopicMessagesRepository topicMessagesRepository;
    MessageMapper messageMapper;
    ObjectMapper objectMapper;
    NotebookMessagesRepository notebookMessagesRepository;

    public CustomPairModel<Long,List<MessageResponseDto>> getAll(UUID parentId,MessageParentType parentType, MessageFilterDto filterDto){
        Page<? extends MessageRelationEntity> page = null;

        switch (parentType) {
            case TOPIC -> {
                topicService.validateTopicOfUser(parentId, JwtUtil.getUserId());
                Specification<TopicMessageEntity> spec = (root, query, criteriaBuilder) -> {
                    boolean isCountQuery = query.getResultType() == Long.class || query.getResultType() == long.class;

                    // Sử dụng interface From làm gốc để có thể tạo Predicate từ cả Join và Fetch
                    From<TopicMessageEntity, MessageEntity> messageNode;

                    if (isCountQuery) {
                        messageNode = root.join("message", JoinType.INNER);
                    } else {
                        // Cú pháp ép kiểu 2 lần để tránh lỗi "Inconvertible types"
                        messageNode = (Join<TopicMessageEntity, MessageEntity>) (Object) root.fetch("message", JoinType.INNER);
                    }

                    // Bây giờ messageNode đã là kiểu From, hoàn toàn hợp lệ để truyền vào createSpec
                    Predicate messageSearch = filterDto.createSpec((Join<TopicMessageEntity, MessageEntity>) messageNode, criteriaBuilder);
                    Predicate topicIdSearch = criteriaBuilder.equal(root.get("topic").get("id"), parentId);

                    return criteriaBuilder.and(messageSearch, topicIdSearch);
                };
                filterDto.setSortPrefix("message");
                page = topicMessagesRepository.findAll(spec,filterDto.createPageable());
            }
            case NOTEBOOK -> {
                noteBookService.validateNoteBookOfUser(parentId, JwtUtil.getUserId());
                Specification<NotebookMessageEntity> spec = (root, query, criteriaBuilder) -> {
                    boolean isCountQuery = query.getResultType() == Long.class || query.getResultType() == long.class;

                    // Sử dụng interface From làm gốc để có thể tạo Predicate từ cả Join và Fetch
                    From<NotebookMessageEntity, MessageEntity> messageNode;

                    if (isCountQuery) {
                        messageNode = root.join("message", JoinType.INNER);
                    } else {
                        // Cú pháp ép kiểu 2 lần để tránh lỗi "Inconvertible types"
                        messageNode = (Join<NotebookMessageEntity, MessageEntity>) (Object) root.fetch("message", JoinType.INNER);
                    }

                    // Bây giờ messageNode đã là kiểu From, hoàn toàn hợp lệ để truyền vào createSpec
                    Predicate messageSearch = filterDto.createSpec((Join<NotebookMessageEntity, MessageEntity>) messageNode, criteriaBuilder);
                    Predicate notebookIdSearch = criteriaBuilder.equal(root.get("notebook").get("id"), parentId);

                    return criteriaBuilder.and(messageSearch, notebookIdSearch);
                };


                filterDto.setSortPrefix("message");
                page = notebookMessagesRepository.findAll(spec,filterDto.createPageable());
            }
        }
        if(page == null)
            throw new AppException(ApiResponseStatus.UNEXPECTED);

        List<MessageResponseDto> messages = page.getContent().stream().map(entity -> {
            MessageResponseDto responseDto = messageMapper.entityToResponseDto(entity.getMessageEntity());
            responseDto.setParentId(parentId);
            responseDto.setParentType(parentType.getValue());
            return responseDto;
        }).toList();

        return new CustomPairModel<>(page.getTotalElements(),messages);
    }

    @Transactional(readOnly = true)
    public List<MessageResponseDto> getTopicMessagesAfter(UUID topicId, UUID messageId) {
        topicService.validateTopicOfUser(topicId, JwtUtil.getUserId());
        return getTopicMessagesAfterInternal(topicId, messageId);
    }

    @Transactional(readOnly = true)
    public List<MessageResponseDto> getTopicMessagesAfterInternal(UUID topicId, UUID messageId) {
        List<TopicMessageEntity> topicMessages = messageId == null
                ? topicMessagesRepository.findByTopic_IdOrderById_MessageIdAsc(topicId)
                : topicMessagesRepository.findByTopic_IdAndId_MessageIdGreaterThanOrderById_MessageIdAsc(topicId, messageId);

        return mapTopicMessages(topicMessages, topicId);
    }

    private List<MessageResponseDto> mapTopicMessages(List<TopicMessageEntity> topicMessages, UUID topicId) {
        return topicMessages.stream().map(entity -> {
            MessageResponseDto responseDto = messageMapper.entityToResponseDto(entity.getMessageEntity());
            responseDto.setParentId(topicId);
            responseDto.setParentType(MessageParentType.TOPIC.getValue());
            return responseDto;
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<MessageResponseDto> getNoteBookMessagesAfter(UUID noteBookId, UUID messageId) {
        noteBookService.validateNoteBookOfUser(noteBookId, JwtUtil.getUserId());
        return getNoteBookMessagesAfterInternal(noteBookId, messageId);
    }

    @Transactional(readOnly = true)
    public List<MessageResponseDto> getNoteBookMessagesAfterInternal(UUID noteBookId, UUID messageId) {
        List<NotebookMessageEntity> noteBookMessages = messageId == null
                ? notebookMessagesRepository.findByNotebook_IdOrderById_MessageIdAsc(noteBookId)
                : notebookMessagesRepository.findByNotebook_IdAndId_MessageIdGreaterThanOrderById_MessageIdAsc(noteBookId, messageId);

        return mapNoteBookMessages(noteBookMessages, noteBookId);
    }

    private List<MessageResponseDto> mapNoteBookMessages(List<NotebookMessageEntity> noteBookMessages, UUID noteBookId) {
        return noteBookMessages.stream().map(entity -> {
            MessageResponseDto responseDto = messageMapper.entityToResponseDto(entity.getMessageEntity());
            responseDto.setParentId(noteBookId);
            responseDto.setParentType(MessageParentType.NOTEBOOK.getValue());
            return responseDto;
        }).toList();
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
                noteBookService.validateNoteBookOfUser(parentId, JwtUtil.getUserId());
                NoteBookEntity topicEntity = noteBookService.getEntityById(parentId);
                notebookMessagesRepository.save(new NotebookMessageEntity(topicEntity,newEntity));
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

    public MessageResponseDto updateTopicMessageFeedback(UUID topicId, UUID messageId, String feedbackValue) {
        topicService.validateTopicOfUser(topicId, JwtUtil.getUserId());
        if (!topicMessagesRepository.existsByTopic_IdAndMessage_Id(topicId, messageId)) {
            throw new AppException(ApiResponseStatus.MESSAGE_ID_NOT_EXISTS);
        }

        return updateFeedback(messageId, feedbackValue);
    }

    public MessageResponseDto updateNoteBookMessageFeedback(UUID noteBookId, UUID messageId, String feedbackValue) {
        noteBookService.validateNoteBookOfUser(noteBookId, JwtUtil.getUserId());
        if (!notebookMessagesRepository.existsByNotebook_IdAndMessage_Id(noteBookId, messageId)) {
            throw new AppException(ApiResponseStatus.MESSAGE_ID_NOT_EXISTS);
        }

        return updateFeedback(messageId, feedbackValue);
    }

    @Transactional(readOnly = true)
    public CustomPairModel<Long, List<MessageFeedbackHistoryResponseDto>> getTopicMessageFeedbackHistory(UUID topicId, UUID messageId, int pageNumber, int pageSize) {
        topicService.validateTopicOfUser(topicId, JwtUtil.getUserId());
        if (!topicMessagesRepository.existsByTopic_IdAndMessage_Id(topicId, messageId)) {
            throw new AppException(ApiResponseStatus.MESSAGE_ID_NOT_EXISTS);
        }

        return getMessageFeedbackHistory(messageId, pageNumber, pageSize);
    }

    @Transactional(readOnly = true)
    public CustomPairModel<Long, List<MessageFeedbackHistoryResponseDto>> getNoteBookMessageFeedbackHistory(UUID noteBookId, UUID messageId, int pageNumber, int pageSize) {
        noteBookService.validateNoteBookOfUser(noteBookId, JwtUtil.getUserId());
        if (!notebookMessagesRepository.existsByNotebook_IdAndMessage_Id(noteBookId, messageId)) {
            throw new AppException(ApiResponseStatus.MESSAGE_ID_NOT_EXISTS);
        }

        return getMessageFeedbackHistory(messageId, pageNumber, pageSize);
    }

    private MessageResponseDto updateFeedback(UUID messageId, String feedbackValue) {
        MessageEntity entity = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.MESSAGE_ID_NOT_EXISTS));

        String normalizedFeedback = normalizeFeedback(feedbackValue);
        String previousFeedback = entity.getFeedback();

        if (isSameFeedback(previousFeedback, normalizedFeedback)) {
            throw new AppException(ApiResponseStatus.MESSAGE_FEEDBACK_NO_CHANGE);
        }

        entity.setFeedback(normalizedFeedback);
        MessageEntity updated = messageRepository.save(entity);

        MessageFeedbackHistoryEntity history = new MessageFeedbackHistoryEntity();
        history.setMessage(updated);
        history.setBeforeFeedback(previousFeedback);
        history.setAfterFeedback(normalizedFeedback);
        messageFeedbackHistoryRepository.save(history);

        return messageMapper.entityToResponseDto(updated);
    }

        private CustomPairModel<Long, List<MessageFeedbackHistoryResponseDto>> getMessageFeedbackHistory(UUID messageId, int pageNumber, int pageSize) {
        int normalizedPageNumber = Math.max(pageNumber, 0);
        int normalizedPageSize = pageSize <= 0
            ? DEFAULT_HISTORY_PAGE_SIZE
            : Math.min(pageSize, MAX_HISTORY_PAGE_SIZE);

        Page<MessageFeedbackHistoryEntity> page = messageFeedbackHistoryRepository
            .findByMessage_IdOrderByAudit_CreatedAtDesc(
                messageId,
                PageRequest.of(normalizedPageNumber, normalizedPageSize));

        List<MessageFeedbackHistoryResponseDto> data = page.getContent().stream()
            .map(this::mapFeedbackHistory)
            .toList();

        return new CustomPairModel<>(page.getTotalElements(), data);
    }

    private MessageFeedbackHistoryResponseDto mapFeedbackHistory(MessageFeedbackHistoryEntity historyEntity) {
        MessageFeedbackHistoryResponseDto responseDto = new MessageFeedbackHistoryResponseDto();
        responseDto.setId(historyEntity.getId());
        responseDto.setMessageId(historyEntity.getMessage().getId());
        responseDto.setBeforeFeedback(historyEntity.getBeforeFeedback());
        responseDto.setAfterFeedback(historyEntity.getAfterFeedback());
        responseDto.setCreatedAt(formatInstant(historyEntity.getAudit().getCreatedAt()));
        responseDto.setCreatedBy(historyEntity.getAudit().getCreatedBy());
        responseDto.setUpdatedAt(formatInstant(historyEntity.getAudit().getUpdatedAt()));
        responseDto.setUpdatedBy(historyEntity.getAudit().getUpdatedBy());
        return responseDto;
    }

    private String formatInstant(Instant value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    private String normalizeFeedback(String feedbackValue) {
        if (feedbackValue == null) {
            return null;
        }

        String normalizedFeedback = feedbackValue.trim().toLowerCase();
        if (normalizedFeedback.isEmpty()) {
            return null;
        }

        if (!MessageFeedbackType.isSupportedValue(normalizedFeedback)) {
            throw new AppException(ApiResponseStatus.INVALID_MESSAGE_FEEDBACK_VALUE);
        }

        return normalizedFeedback;
    }

    private boolean isSameFeedback(String oldValue, String newValue) {
        if (oldValue == null && newValue == null) {
            return true;
        }

        if (oldValue == null || newValue == null) {
            return false;
        }

        return oldValue.equalsIgnoreCase(newValue);
    }

    public MessageResponseDto update(UUID messageId, MessageUpdateRequestDto updateRequestDto){
        MessageEntity entity = messageRepository.findById(messageId).orElseThrow(()-> new AppException(ApiResponseStatus.MESSAGE_ID_NOT_EXISTS));
        messageMapper.updateEntity(entity,updateRequestDto);
        return messageMapper.entityToResponseDto(messageRepository.save(entity));
    }
}
