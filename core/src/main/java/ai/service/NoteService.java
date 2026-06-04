package ai.service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import ai.annotation.Audited;
import ai.dto.own.request.NoteCreateByNoteBookRequestDto;
import ai.dto.own.request.NoteCreateByTopicRequestDto;
import ai.dto.own.request.NoteCreateRequestDto;
import ai.dto.own.request.NoteUpdateRequestDto;
import ai.dto.own.request.filter.NoteFilterDto;
import ai.dto.own.response.NoteResponseDto;
import ai.entity.postgres.NoteBookEntity;
import ai.entity.postgres.NoteEntity;
import ai.entity.postgres.TopicEntity;
import ai.enums.ApiResponseStatus;
import ai.enums.AuditAction;
import ai.enums.AuditResource;
import ai.enums.NoteSourceBy;
import ai.enums.NoteSourceType;
import ai.exeption.AppException;
import ai.mapper.NoteMapper;
import ai.model.CustomPairModel;
import ai.repository.NoteRepository;
import ai.util.JwtUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class NoteService {
    NoteRepository noteRepository;
    NoteMapper noteMapper;
    UserService userService;
    OrganizationService organizationService;
    TopicService topicService;
    NoteBookService noteBookService;

    public void validateNoteOfUser(UUID noteId, UUID userId) {
        if (!noteRepository.existsByIdAndOwnerId(noteId, userId)) {
            throw new AppException(ApiResponseStatus.PERMISSION_DENIED);
        }
    }

    public void validateNoteOfOrganization(UUID noteId, UUID organizationId) {
        if (!noteRepository.existsByIdAndOrganizationId(noteId, organizationId)) {
            throw new AppException(ApiResponseStatus.PERMISSION_DENIED);
        }
    }

    public NoteEntity getEntityById(UUID noteId) {
        return noteRepository.findById(noteId).orElseThrow(() -> new AppException(ApiResponseStatus.NOTE_NOT_EXISTS));
    }

    public CustomPairModel<Long, List<NoteResponseDto>> getAll(NoteFilterDto filterDto) {
        UUID userId = JwtUtil.getUserId();
        UUID organizationId = JwtUtil.getOrgId();

        Specification<NoteEntity> spec = filterDto.createSpec().and((root, query, criteriaBuilder) -> {
            Predicate orgIdPredicate = criteriaBuilder.equal(root.get("organization").get("id"), organizationId);
            Predicate ownerPredicate = criteriaBuilder.equal(root.get("owner").get("id"), userId);
            return criteriaBuilder.and(orgIdPredicate, ownerPredicate);
        });

        Page<NoteEntity> page = noteRepository.findAll(spec, filterDto.createPageable());

        return new CustomPairModel<>(
                page.getTotalElements(),
                page.getContent().stream().map(noteMapper::entityToResponseDto).toList());
    }

    @Audited(action = AuditAction.CREATE, resource = AuditResource.NOTE, description = "Tạo ghi chú: {0}")
    public NoteResponseDto create(NoteCreateRequestDto requestDto) {
        UUID userId = JwtUtil.getUserId();
        UUID orgId = JwtUtil.getOrgId();

        NoteEntity note = new NoteEntity();
        note.setTitle(normalizeNullable(requestDto.getTitle()));
        note.setContent(normalizeRequired(requestDto.getContent()));
        note.setSourceType(parseSourceType(NoteSourceType.USER.name()));
        note.setSourceBy(parseSourceBy(NoteSourceBy.HUMAN.name()));
        note.setOwner(userService.getEntityById(userId));
        note.setOrganization(organizationService.getEntityById(orgId));

        return noteMapper.entityToResponseDto(noteRepository.save(note));
    }

    public NoteResponseDto create(NoteCreateByTopicRequestDto requestDto) {
        UUID userId = JwtUtil.getUserId();
        UUID orgId = JwtUtil.getOrgId();

        TopicEntity topic = topicService.getEntityById(UUID.fromString(requestDto.getTopicId()));

        NoteEntity note = new NoteEntity();
        note.setTitle(normalizeNullable(requestDto.getTitle()));
        note.setContent(normalizeRequired(requestDto.getContent()));
        note.setSourceType(parseSourceType(NoteSourceType.TOPIC.name()));
        note.setTopicId(topic.getId());
        note.setSourceBy(parseSourceBy(NoteSourceBy.AGENT.name()));
        note.setOwner(userService.getEntityById(userId));
        note.setOrganization(organizationService.getEntityById(orgId));

        return noteMapper.entityToResponseDto(noteRepository.save(note));
    }

    public NoteResponseDto create(NoteCreateByNoteBookRequestDto requestDto) {
        UUID userId = JwtUtil.getUserId();
        UUID orgId = JwtUtil.getOrgId();

        NoteBookEntity noteBook = noteBookService.getEntityById(UUID.fromString(requestDto.getNoteBookId()));

        NoteEntity note = new NoteEntity();
        note.setTitle(normalizeNullable(requestDto.getTitle()));
        note.setContent(normalizeRequired(requestDto.getContent()));
        note.setSourceType(parseSourceType(NoteSourceType.NOTEBOOK.name()));
        note.setSourceBy(parseSourceBy(requestDto.getSourceBy()));
        note.setNoteBookId(noteBook.getId());
        note.setOwner(userService.getEntityById(userId));
        note.setOrganization(organizationService.getEntityById(orgId));

        return noteMapper.entityToResponseDto(noteRepository.save(note));
    }

    public NoteResponseDto getById(UUID noteId) {
        UUID userId = JwtUtil.getUserId();
        validateNoteOfUser(noteId, userId);
        return noteMapper.entityToResponseDto(getEntityById(noteId));
    }

    @Audited(action = AuditAction.UPDATE, resource = AuditResource.NOTE, resourceIdExpression = "#arg0", description = "Cập nhật ghi chú: {0}")
    public NoteResponseDto update(UUID noteId, NoteUpdateRequestDto requestDto) {
        UUID userId = JwtUtil.getUserId();
        UUID orgId = JwtUtil.getOrgId();

        validateNoteOfUser(noteId, userId);
        validateNoteOfOrganization(noteId, orgId);

        NoteEntity note = getEntityById(noteId);
        if(note.getSourceBy()!=null && note.getSourceBy() == NoteSourceBy.AGENT) {
            throw new AppException(ApiResponseStatus.NOTE_SOURCE_BY_NOT_ALLOW_UPDATE);
        } else {
            note.setSourceBy(NoteSourceBy.HUMAN);
        }

        note.setTitle(normalizeNullable(requestDto.getTitle()));
        note.setContent(normalizeRequired(requestDto.getContent()));

        return noteMapper.entityToResponseDto(noteRepository.save(note));
    }

    @Audited(action = AuditAction.DELETE, resource = AuditResource.NOTE, resourceIdExpression = "#arg0", description = "Xoá ghi chú: {0}")
    public void delete(UUID noteId) {
        UUID userId = JwtUtil.getUserId();
        validateNoteOfUser(noteId, userId);
        noteRepository.deleteById(noteId);
    }

    private NoteSourceType parseSourceType(String sourceType) {
        try {
            return NoteSourceType.valueOf(sourceType.trim().toUpperCase(Locale.ROOT));
        } catch (Exception exception) {
            throw new AppException(ApiResponseStatus.NOTE_SOURCE_TYPE_INVALID);
        }
    }

    private NoteSourceBy parseSourceBy(String sourceBy) {
        try {
            return NoteSourceBy.valueOf(sourceBy.trim().toUpperCase(Locale.ROOT));
        } catch (Exception exception) {
            throw new AppException(ApiResponseStatus.NOTE_SOURCE_BY_INVALID);
        }
    }

    private String normalizeRequired(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new AppException(ApiResponseStatus.NOTE_CONTENT_CAN_NOT_BE_NULL_OR_EMPTY);
        }
        return value.trim();
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    
}
