package ai.service;

import ai.dto.own.request.NoteCreateRequestDto;
import ai.dto.own.request.NoteUpdateRequestDto;
import ai.dto.own.request.filter.NoteFilterDto;
import ai.dto.own.response.NoteResponseDto;
import ai.entity.postgres.NoteEntity;
import ai.enums.ApiResponseStatus;
import ai.enums.NoteSourceType;
import ai.exeption.AppException;
import ai.mapper.NoteMapper;
import ai.model.CustomPairModel;
import ai.repository.NoteRepository;
import ai.util.JwtUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class NoteService {
    NoteRepository noteRepository;
    NoteMapper noteMapper;
    UserService userService;
    TopicService topicService;
    NoteBookService noteBookService;

    public void validateNoteOfUser(UUID noteId, UUID userId) {
        if (!noteRepository.existsByIdAndOwnerId(noteId, userId)) {
            throw new AppException(ApiResponseStatus.PERMISSION_DENIED);
        }
    }

    public NoteEntity getEntityById(UUID noteId) {
        return noteRepository.findById(noteId).orElseThrow(() -> new AppException(ApiResponseStatus.NOTE_NOT_EXISTS));
    }

    public CustomPairModel<Long, List<NoteResponseDto>> getAll(NoteFilterDto filterDto) {
        UUID userId = JwtUtil.getUserId();
        userService.validateUserId(userId);

        Specification<NoteEntity> spec = filterDto.createSpec().and(
                (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("owner").get("id"), userId));

        Page<NoteEntity> page = noteRepository.findAll(spec, filterDto.createPageable());

        return new CustomPairModel<>(
                page.getTotalElements(),
                page.getContent().stream().map(noteMapper::entityToResponseDto).toList());
    }

    public NoteResponseDto create(NoteCreateRequestDto requestDto) {
        UUID userId = JwtUtil.getUserId();

        NoteEntity note = new NoteEntity();
        note.setTitle(normalizeNullable(requestDto.getTitle()));
        note.setContent(normalizeRequired(requestDto.getContent()));
        note.setSourceType(parseSourceType(requestDto.getSourceType()));
        note.setOwner(userService.getEntityById(userId));

        applySourceTarget(note, requestDto.getTopicId(), requestDto.getNoteBookId(), userId);

        return noteMapper.entityToResponseDto(noteRepository.save(note));
    }

    public NoteResponseDto getById(UUID noteId) {
        UUID userId = JwtUtil.getUserId();
        validateNoteOfUser(noteId, userId);
        return noteMapper.entityToResponseDto(getEntityById(noteId));
    }

    public NoteResponseDto update(UUID noteId, NoteUpdateRequestDto requestDto) {
        UUID userId = JwtUtil.getUserId();
        validateNoteOfUser(noteId, userId);

        NoteEntity note = getEntityById(noteId);
        note.setTitle(normalizeNullable(requestDto.getTitle()));
        note.setContent(normalizeRequired(requestDto.getContent()));
        note.setSourceType(parseSourceType(requestDto.getSourceType()));

        applySourceTarget(note, requestDto.getTopicId(), requestDto.getNoteBookId(), userId);

        return noteMapper.entityToResponseDto(noteRepository.save(note));
    }

    public void delete(UUID noteId) {
        UUID userId = JwtUtil.getUserId();
        validateNoteOfUser(noteId, userId);
        noteRepository.deleteById(noteId);
    }

    private void applySourceTarget(NoteEntity note, UUID topicId, UUID noteBookId, UUID userId) {
        if (note.getSourceType() == NoteSourceType.TOPIC) {
            if (topicId == null) {
                throw new AppException(ApiResponseStatus.NOTE_TOPIC_ID_REQUIRED);
            }
            topicService.validateTopicOfUser(topicId, userId);
            note.setTopicId(topicId);
            note.setNoteBookId(null);
            return;
        }

        if (note.getSourceType() == NoteSourceType.NOTEBOOK) {
            if (noteBookId == null) {
                throw new AppException(ApiResponseStatus.NOTE_NOTEBOOK_ID_REQUIRED);
            }
            noteBookService.validateNoteBookOfUser(noteBookId, userId);
            note.setTopicId(null);
            note.setNoteBookId(noteBookId);
            return;
        }

        throw new AppException(ApiResponseStatus.NOTE_SOURCE_TARGET_INVALID);
    }

    private NoteSourceType parseSourceType(String sourceType) {
        try {
            return NoteSourceType.valueOf(sourceType.trim().toUpperCase(Locale.ROOT));
        } catch (Exception exception) {
            throw new AppException(ApiResponseStatus.NOTE_SOURCE_TYPE_INVALID);
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
