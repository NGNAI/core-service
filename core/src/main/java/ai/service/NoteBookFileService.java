package ai.service;

import ai.dto.own.request.DataIngestionUploadRequestDto;
import ai.dto.own.response.DataIngestionDownloadData;
import ai.dto.own.response.DataIngestionPresignedUrlResponseDto;
import ai.dto.own.response.NoteBookFileResponseDto;
import ai.entity.postgres.DataIngestionEntity;
import ai.entity.postgres.NoteBookEntity;
import ai.entity.postgres.NoteBookFileEntity;
import ai.enums.ApiResponseStatus;
import ai.enums.DataScope;
import ai.enums.DataSource;
import ai.exeption.AppException;
import ai.mapper.NoteBookFileMapper;
import ai.model.CustomPairModel;
import ai.repository.NoteBookFileRepository;
import ai.util.JwtUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Service
public class NoteBookFileService {
    NoteBookFileRepository noteBookFileRepository;
    NoteBookFileMapper noteBookFileMapper;
    NoteBookService noteBookService;
    DataIngestionService dataIngestionService;

    public CustomPairModel<Long, List<NoteBookFileResponseDto>> getFiles(UUID noteBookId, int page, int size) {
        noteBookService.validateNoteBookOfUser(noteBookId, JwtUtil.getUserId());
        Page<NoteBookFileEntity> result = noteBookFileRepository.findByNoteBookId(noteBookId, PageRequest.of(page, size));
        return new CustomPairModel<>(result.getTotalElements(),
                result.getContent().stream().map(noteBookFileMapper::entityToResponseDto).toList());
    }

    public List<NoteBookFileResponseDto> uploadFilesAndWaitForCompletion(UUID noteBookId, MultipartFile[] files) {
        UUID userId = JwtUtil.getUserId();
        UUID orgId = JwtUtil.getOrgId();

        noteBookService.validateNoteBookOfUser(noteBookId, userId);

        List<MultipartFile> validFiles = Arrays.stream(files == null ? new MultipartFile[0] : files)
                .filter(file -> file != null && !file.isEmpty())
                .toList();

        if (validFiles.isEmpty()) {
            return List.of();
        }

        int poolSize = Math.min(validFiles.size(), Math.max(1, Runtime.getRuntime().availableProcessors()));
        ExecutorService executorService = Executors.newFixedThreadPool(poolSize);

        try {
            List<CompletableFuture<NoteBookFileResponseDto>> futures = validFiles.stream()
                    .map(file -> CompletableFuture.supplyAsync(
                            () -> uploadSingleFileAndAttach(noteBookId, file, userId, orgId),
                            executorService))
                    .toList();

            return futures.stream().map(future -> {
                try {
                    return future.join();
                } catch (CompletionException exception) {
                    throw unwrapCompletionException(exception);
                }
            }).toList();
        } finally {
            executorService.shutdown();
        }
    }

    @Transactional
    public NoteBookFileResponseDto addFile(UUID noteBookId, UUID fileId, String summary, String metadata) {
        return addFile(noteBookId, fileId, summary, metadata, JwtUtil.getUserId());
    }

    @Transactional
    public NoteBookFileResponseDto addFile(UUID noteBookId, UUID fileId, String summary, String metadata, UUID userId) {
        noteBookService.validateNoteBookOfUser(noteBookId, userId);

        if (noteBookFileRepository.existsByNoteBookIdAndDataIngestionId(noteBookId, fileId)) {
            throw new AppException(ApiResponseStatus.NOTEBOOK_FILE_ALREADY_EXISTS);
        }

        NoteBookEntity noteBook = noteBookService.getEntityById(noteBookId);
        DataIngestionEntity dataIngestion = dataIngestionService.getCompletedFileEntityById(fileId);

        NoteBookFileEntity entity = NoteBookFileEntity.builder()
                .noteBook(noteBook)
                .dataIngestion(dataIngestion)
                .summary(summary)
                .metadata(metadata)
                .build();

        return noteBookFileMapper.entityToResponseDto(noteBookFileRepository.save(entity));
    }

    private NoteBookFileResponseDto uploadSingleFileAndAttach(UUID noteBookId, MultipartFile file, UUID userId, UUID orgId) {
        DataIngestionUploadRequestDto requestDto = new DataIngestionUploadRequestDto();
        requestDto.setFile(file);
        requestDto.setAccessLevel(DataScope.PERSONAL);

        UUID dataIngestionId = dataIngestionService.uploadDataIngestion(requestDto, userId, orgId, DataSource.NOTEBOOK).getId();
        dataIngestionService.waitForIngestionCompleted(dataIngestionId);
        return addFile(noteBookId, dataIngestionId, null, null, userId);
    }

    private RuntimeException unwrapCompletionException(CompletionException exception) {
        Throwable cause = exception;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }

        if (cause instanceof RuntimeException runtimeException) {
            return runtimeException;
        }

        return new AppException(ApiResponseStatus.UNEXPECTED);
    }

    @Transactional
    public void removeFile(UUID noteBookId, UUID fileId) {
        noteBookService.validateNoteBookOfUser(noteBookId, JwtUtil.getUserId());

        NoteBookFileEntity entity = noteBookFileRepository.findByNoteBookIdAndDataIngestionId(noteBookId, fileId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.NOTEBOOK_FILE_NOT_EXISTS));

        noteBookFileRepository.delete(entity);
    }

    public DataIngestionDownloadData downloadFile(UUID noteBookId, UUID fileId) {
        noteBookService.validateNoteBookOfUser(noteBookId, JwtUtil.getUserId());
        
        NoteBookFileEntity noteBookFileEntity = noteBookFileRepository.findByNoteBookIdAndDataIngestionId(noteBookId, fileId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.NOTEBOOK_FILE_NOT_EXISTS));

        DataIngestionEntity dataIngestionEntity = noteBookFileEntity.getDataIngestion();
        return dataIngestionService.downloadById(dataIngestionEntity.getId());
    }

    public DataIngestionPresignedUrlResponseDto getDownloadUrl(UUID noteBookId, UUID fileId, Integer expiresInSeconds) {
        noteBookService.validateNoteBookOfUser(noteBookId, JwtUtil.getUserId());
        
        NoteBookFileEntity noteBookFileEntity = noteBookFileRepository.findByNoteBookIdAndDataIngestionId(noteBookId, fileId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.NOTEBOOK_FILE_NOT_EXISTS));

        DataIngestionEntity dataIngestionEntity = noteBookFileEntity.getDataIngestion();
        return dataIngestionService.getPresignedDownloadUrl(dataIngestionEntity.getId(), expiresInSeconds);
    }
}
