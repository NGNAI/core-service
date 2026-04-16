package ai.service;

import ai.dto.own.request.NoteBookCreateRequestDto;
import ai.dto.own.request.NoteBookRenameTitleRequestDto;
import ai.dto.own.request.filter.NoteBookFilterDto;
import ai.dto.own.response.NoteBookResponseDto;
import ai.entity.postgres.NoteBookEntity;
import ai.enums.ApiResponseStatus;
import ai.exeption.AppException;
import ai.mapper.NoteBookMapper;
import ai.model.CustomPairModel;
import ai.repository.NoteBookRepository;
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
public class NoteBookService {
    NoteBookRepository noteBookRepository;
    UserService userService;

    NoteBookMapper noteBookMapper;

    public void validateNoteBookOfUser(UUID NoteBookId, UUID userId){
        if(!noteBookRepository.existsByIdAndOwnerId(NoteBookId,userId))
            throw new AppException(ApiResponseStatus.PERMISSION_DENIED);
    }

    public void validateNoteBookId(UUID NoteBookId){
        if(!noteBookRepository.existsById(NoteBookId))
            throw new AppException(ApiResponseStatus.NOTEBOOK_ID_NOT_EXISTS);
    }

    public NoteBookEntity getEntityById(UUID NoteBookId){
        return noteBookRepository.findById(NoteBookId).orElseThrow(() -> new AppException(ApiResponseStatus.NOTEBOOK_ID_NOT_EXISTS));
    }

    public CustomPairModel<Long,List<NoteBookResponseDto>> getAll(NoteBookFilterDto filterDto){
        UUID userId = JwtUtil.getUserId();
        userService.validateUserId(userId);
        Specification<NoteBookEntity> spec = filterDto.createSpec().and((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("owner").get("id"), userId));

        Page<NoteBookEntity> page = noteBookRepository.findAll(
                spec,
                filterDto.createPageable()
        );

        return new CustomPairModel<>(page.getTotalElements(),page.getContent().stream().map(noteBookMapper::entityToResponseDto).toList());
    }

    public NoteBookResponseDto create(NoteBookCreateRequestDto createRequestDto){
        NoteBookEntity newEntity = noteBookMapper.createRequestDtoToEntity(createRequestDto);
        newEntity.setOwner(userService.getEntityById(JwtUtil.getUserId()));

        return noteBookMapper.entityToResponseDto(noteBookRepository.save(newEntity));
    }

    public NoteBookResponseDto renameTitle(UUID id, NoteBookRenameTitleRequestDto requestDto){
        validateNoteBookOfUser(id,JwtUtil.getUserId());
        NoteBookEntity entity = noteBookRepository.findById(id).orElseThrow(() -> new AppException(ApiResponseStatus.NOTEBOOK_ID_NOT_EXISTS));
        entity.setTitle(requestDto.getTitle());

        return noteBookMapper.entityToResponseDto(noteBookRepository.save(entity));
    }

    public void delete(UUID id){
        validateNoteBookOfUser(id,JwtUtil.getUserId());
        noteBookRepository.deleteById(id);
    }
}
