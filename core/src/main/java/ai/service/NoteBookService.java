package ai.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import ai.annotation.Audited;
import ai.dto.own.request.NoteBookCreateRequestDto;
import ai.dto.own.request.NoteBookRenameTitleRequestDto;
import ai.dto.own.request.NoteBookUpdateInstructionRequestDto;
import ai.dto.own.request.NoteBookUpdateRequestDto;
import ai.dto.own.request.filter.NoteBookFilterDto;
import ai.dto.own.response.NoteBookResponseDto;
import ai.entity.postgres.NoteBookEntity;
import ai.enums.ApiResponseStatus;
import ai.enums.AuditAction;
import ai.enums.AuditResource;
import ai.exception.AppException;
import ai.mapper.NoteBookMapper;
import ai.model.CustomPairModel;
import ai.repository.NoteBookRepository;
import ai.util.JwtUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class NoteBookService {
    NoteBookRepository noteBookRepository;
    UserService userService;
    OrganizationService organizationService;
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
        UUID organizationId = JwtUtil.getOrgId();

        userService.validateUserId(userId);
        organizationService.validateOrgId(organizationId);

        Specification<NoteBookEntity> spec = filterDto.createSpec().and((root, query, criteriaBuilder) -> {
            Predicate orgIdPredicate = criteriaBuilder.equal(root.get("organization").get("id"), organizationId);
            Predicate ownerPredicate = criteriaBuilder.equal(root.get("owner").get("id"), userId);
            return criteriaBuilder.and(orgIdPredicate, ownerPredicate);
        });

        Page<NoteBookEntity> page = noteBookRepository.findAll(
                spec,
                filterDto.createPageable()
        );

        return new CustomPairModel<>(page.getTotalElements(),page.getContent().stream().map(noteBookMapper::entityToResponseDto).toList());
    }

    @Audited(action = AuditAction.CREATE, resource = AuditResource.NOTEBOOK, description = "Tạo sổ tay: {0}")
    public NoteBookResponseDto create(NoteBookCreateRequestDto createRequestDto){
        NoteBookEntity newEntity = noteBookMapper.createRequestDtoToEntity(createRequestDto);
        newEntity.setOwner(userService.getEntityById(JwtUtil.getUserId()));
        newEntity.setOrganization(organizationService.getEntityById(JwtUtil.getOrgId()));

        return noteBookMapper.entityToResponseDto(noteBookRepository.save(newEntity));
    }

    public NoteBookResponseDto getById(UUID id){
        validateNoteBookOfUser(id,JwtUtil.getUserId());
        return noteBookMapper.entityToResponseDto(noteBookRepository.findById(id).orElseThrow(() -> new AppException(ApiResponseStatus.NOTEBOOK_ID_NOT_EXISTS)));
    }

    @Audited(action = AuditAction.UPDATE, resource = AuditResource.NOTEBOOK, resourceIdExpression = "#arg0", description = "Cập nhật sổ tay: {0}")
    public NoteBookResponseDto update(UUID id, NoteBookUpdateRequestDto requestDto){
        validateNoteBookOfUser(id,JwtUtil.getUserId());
        NoteBookEntity entity = noteBookRepository.findById(id).orElseThrow(() -> new AppException(ApiResponseStatus.NOTEBOOK_ID_NOT_EXISTS));
        entity.setTitle(requestDto.getTitle());
        entity.setDescription(requestDto.getDescription());
        entity.setInstruction(requestDto.getInstruction());

        return noteBookMapper.entityToResponseDto(noteBookRepository.save(entity));
    }

    @Audited(action = AuditAction.UPDATE, resource = AuditResource.NOTEBOOK, resourceIdExpression = "#arg0", description = "Đổi tên sổ tay: {0}")
    public NoteBookResponseDto renameTitle(UUID id, NoteBookRenameTitleRequestDto requestDto){
        validateNoteBookOfUser(id,JwtUtil.getUserId());
        NoteBookEntity entity = noteBookRepository.findById(id).orElseThrow(() -> new AppException(ApiResponseStatus.NOTEBOOK_ID_NOT_EXISTS));
        entity.setTitle(requestDto.getTitle());

        return noteBookMapper.entityToResponseDto(noteBookRepository.save(entity));
    }

    @Audited(action = AuditAction.UPDATE, resource = AuditResource.NOTEBOOK, resourceIdExpression = "#arg0", description = "Cập nhật chỉ dẫn sổ tay: {0}")
    public NoteBookResponseDto updateInstruction(UUID id, NoteBookUpdateInstructionRequestDto requestDto){
        validateNoteBookOfUser(id,JwtUtil.getUserId());
        NoteBookEntity entity = noteBookRepository.findById(id).orElseThrow(() -> new AppException(ApiResponseStatus.NOTEBOOK_ID_NOT_EXISTS));
        entity.setInstruction(requestDto.getInstruction());

        return noteBookMapper.entityToResponseDto(noteBookRepository.save(entity));
    }

    @Audited(action = AuditAction.DELETE, resource = AuditResource.NOTEBOOK, resourceIdExpression = "#arg0", description = "Xoá sổ tay: {0}")
    public void delete(UUID id){
        validateNoteBookOfUser(id,JwtUtil.getUserId());
        noteBookRepository.deleteById(id);
    }

    public void updateConversationSummaryInternal(UUID noteBookId, String summary, UUID lastMessageId) {
        noteBookRepository.findById(noteBookId).ifPresent(entity -> {
            entity.setConversationSummary(summary);
            entity.setConversationSummaryLastMessageId(lastMessageId);
            noteBookRepository.save(entity);
        });
    }

    /**
     * Lấy entity Notebook theo id <b>không kiểm tra ownership</b>.
     * Dùng cho public share link flow (viewer không cần JWT).
     * Chỉ validate notebook tồn tại.
     */
    public NoteBookEntity getEntityByIdShared(UUID noteBookId) {
        return noteBookRepository.findById(noteBookId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.NOTEBOOK_ID_NOT_EXISTS));
    }
}
