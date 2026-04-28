package ai.service;

import ai.dto.own.request.NoteBookFilesAddRequestDto;
import ai.dto.own.response.NoteBookSourceDownloadData;
import ai.dto.own.response.NoteBookSourcePresignedUrlResponseDto;
import ai.dto.own.response.NoteBookSourceResponseDto;
import ai.entity.postgres.NoteEntity;
import ai.entity.postgres.NoteBookEntity;
import ai.entity.postgres.NoteBookSourceEntity;
import ai.enums.ApiResponseStatus;
import ai.exeption.AppException;
import ai.mapper.NoteBookSourceMapper;
import ai.model.CustomPairModel;
import ai.repository.NoteBookSourceRepository;
import ai.util.JwtUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.ArrayList;
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
public class NoteBookSourceService {
    static final String NOTEBOOK_BUCKET = "notebookllm";
    static final int DEFAULT_PRESIGNED_EXPIRY_SECONDS = 900;

    NoteBookSourceRepository noteBookSourceRepository;
    NoteBookSourceMapper noteBookSourceMapper;
    NoteBookService noteBookService;
    NoteService noteService;
    MinioService minioService;

    public CustomPairModel<Long, List<NoteBookSourceResponseDto>> getSources(UUID noteBookId, int page, int size) {
        noteBookService.validateNoteBookOfUser(noteBookId, JwtUtil.getUserId());
        Page<NoteBookSourceEntity> result = noteBookSourceRepository.findByNoteBookId(noteBookId, PageRequest.of(page, size));
        return new CustomPairModel<>(result.getTotalElements(),
                result.getContent().stream().map(noteBookSourceMapper::entityToResponseDto).toList());
    }

    public List<NoteBookSourceResponseDto> uploadSources(UUID noteBookId, NoteBookFilesAddRequestDto requestDto) {
        UUID userId = JwtUtil.getUserId();
        noteBookService.validateNoteBookOfUser(noteBookId, userId);

        MultipartFile[] files = requestDto == null ? null : requestDto.getFiles();
        List<MultipartFile> validFiles = Arrays.stream(files == null ? new MultipartFile[0] : files)
                .filter(file -> file != null && !file.isEmpty())
                .toList();

        String textContent = requestDto == null ? null : normalizeText(requestDto.getTextContent());
        String textDisplayName = requestDto == null ? null : normalizeText(requestDto.getTextDisplayName());
        UUID noteId = requestDto == null ? null : requestDto.getNoteId();
        String noteDisplayName = requestDto == null ? null : normalizeText(requestDto.getNoteDisplayName());

        if (validFiles.isEmpty() && textContent == null && noteId == null) {
            throw new AppException(ApiResponseStatus.NOTEBOOK_SOURCE_PAYLOAD_REQUIRED);
        }

        List<NoteBookSourceResponseDto> sources = new ArrayList<>();

        if (textContent != null) {
            sources.add(createTextSource(noteBookId, textContent, textDisplayName));
        }

        if (noteId != null) {
            sources.add(createNoteSource(noteBookId, noteId, noteDisplayName));
        }

        if (validFiles.isEmpty()) {
            return sources;
        }

        int poolSize = Math.min(validFiles.size(), Math.max(1, Runtime.getRuntime().availableProcessors()));
        ExecutorService executorService = Executors.newFixedThreadPool(poolSize);

        try {
            List<CompletableFuture<NoteBookSourceResponseDto>> futures = validFiles.stream()
                    .map(file -> CompletableFuture.supplyAsync(
                    () -> uploadSingleFileAndAttach(noteBookId, file, userId),
                            executorService))
                    .toList();

            sources.addAll(futures.stream().map(future -> {
                try {
                    return future.join();
                } catch (CompletionException exception) {
                    throw unwrapCompletionException(exception);
                }
            }).toList());
            return sources;
        } finally {
            executorService.shutdown();
        }
    }

    @Transactional
    protected NoteBookSourceResponseDto createTextSource(UUID noteBookId, String textContent, String textDisplayName) {
        UUID userId = JwtUtil.getUserId();
        noteBookService.validateNoteBookOfUser(noteBookId, userId);

        String displayName = textDisplayName != null
                ? textDisplayName
                : "text-source-" + UUID.randomUUID().toString().substring(0, 8);

        if (noteBookSourceRepository.existsByNoteBookIdAndDisplayNameAndSourceType(
                noteBookId,
                displayName,
                NoteBookSourceEntity.SourceType.TEXT)) {
            throw new AppException(ApiResponseStatus.NOTEBOOK_SOURCE_ALREADY_EXISTS);
        }

        NoteBookEntity noteBook = noteBookService.getEntityById(noteBookId);
        NoteBookSourceEntity entity = NoteBookSourceEntity.builder()
                .noteBook(noteBook)
            .note(null)
                .sourceType(NoteBookSourceEntity.SourceType.TEXT)
                .displayName(displayName)
                .rawContent(textContent)
                .filePath(null)
                .summary(null)
                .metadata(null)
                .vectorStatus(NoteBookSourceEntity.VectorStatus.NOT_PROCESSED)
                .build();

        return noteBookSourceMapper.entityToResponseDto(noteBookSourceRepository.save(entity));
    }

    @Transactional
    protected NoteBookSourceResponseDto createNoteSource(UUID noteBookId, UUID noteId, String noteDisplayName) {
        UUID userId = JwtUtil.getUserId();
        noteBookService.validateNoteBookOfUser(noteBookId, userId);
        noteService.validateNoteOfUser(noteId, userId);

        if (noteBookSourceRepository.existsByNoteBookIdAndNote_IdAndSourceType(
                noteBookId,
                noteId,
                NoteBookSourceEntity.SourceType.NOTE)) {
            throw new AppException(ApiResponseStatus.NOTEBOOK_SOURCE_ALREADY_EXISTS);
        }

        NoteEntity note = noteService.getEntityById(noteId);
        String displayName = noteDisplayName;
        if (displayName == null) {
            displayName = normalizeText(note.getTitle());
        }
        if (displayName == null) {
            displayName = "note-source-" + note.getId().toString().substring(0, 8);
        }

        NoteBookEntity noteBook = noteBookService.getEntityById(noteBookId);
        NoteBookSourceEntity entity = NoteBookSourceEntity.builder()
                .noteBook(noteBook)
                .note(note)
                .sourceType(NoteBookSourceEntity.SourceType.NOTE)
                .displayName(displayName)
                .rawContent(note.getContent())
                .filePath(null)
                .summary(null)
                .metadata(null)
                .vectorStatus(NoteBookSourceEntity.VectorStatus.NOT_PROCESSED)
                .build();

        return noteBookSourceMapper.entityToResponseDto(noteBookSourceRepository.save(entity));
    }

    @Transactional
    public void removeSource(UUID noteBookId, UUID sourceId) {
        noteBookService.validateNoteBookOfUser(noteBookId, JwtUtil.getUserId());
        NoteBookSourceEntity entity = getSourceEntity(noteBookId, sourceId);
        noteBookSourceRepository.delete(entity);
    }

    public NoteBookSourceDownloadData downloadSource(UUID noteBookId, UUID sourceId) {
        noteBookService.validateNoteBookOfUser(noteBookId, JwtUtil.getUserId());
        NoteBookSourceEntity source = getSourceEntity(noteBookId, sourceId);
        validateDownloadableSource(source);

        MinioService.MinioObjectData objectData = minioService.download(source.getFilePath(), NOTEBOOK_BUCKET);
        return new NoteBookSourceDownloadData(resolveFileName(source), objectData.getContentType(), objectData.getBytes());
    }

    public NoteBookSourcePresignedUrlResponseDto getSourceDownloadUrl(UUID noteBookId, UUID sourceId, Integer expiresInSeconds) {
        noteBookService.validateNoteBookOfUser(noteBookId, JwtUtil.getUserId());
        NoteBookSourceEntity source = getSourceEntity(noteBookId, sourceId);
        validateDownloadableSource(source);

        int effectiveExpiry = expiresInSeconds == null || expiresInSeconds <= 0
                ? DEFAULT_PRESIGNED_EXPIRY_SECONDS
                : expiresInSeconds;

        String url = minioService.generatePresignedDownloadUrl(source.getFilePath(), effectiveExpiry, NOTEBOOK_BUCKET);
        return NoteBookSourcePresignedUrlResponseDto.builder()
                .url(url)
                .expiresInSeconds(effectiveExpiry)
                .build();
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

    private NoteBookSourceResponseDto uploadSingleFileAndAttach(UUID noteBookId, MultipartFile file, UUID userId) {
        String originalName = file.getOriginalFilename();
        String displayName = (originalName == null || originalName.isBlank())
                ? "unnamed-source"
                : originalName;

        if (noteBookSourceRepository.existsByNoteBookIdAndDisplayNameAndSourceType(
                noteBookId,
                displayName,
                NoteBookSourceEntity.SourceType.FILE)) {
            throw new AppException(ApiResponseStatus.NOTEBOOK_SOURCE_ALREADY_EXISTS);
        }

        String objectPath = minioService.upload(
                file,
                userId.toString(),
                noteBookId.toString(),
                NOTEBOOK_BUCKET);

        NoteBookEntity noteBook = noteBookService.getEntityById(noteBookId);
        NoteBookSourceEntity entity = NoteBookSourceEntity.builder()
                .noteBook(noteBook)
            .note(null)
                .sourceType(NoteBookSourceEntity.SourceType.FILE)
                .displayName(displayName)
                .rawContent(null)
                .filePath(objectPath)
                .summary(null)
                .metadata(null)
                .vectorStatus(NoteBookSourceEntity.VectorStatus.NOT_PROCESSED)
                .build();

        return noteBookSourceMapper.entityToResponseDto(noteBookSourceRepository.save(entity));
    }

    private NoteBookSourceEntity getSourceEntity(UUID noteBookId, UUID sourceId) {
        return noteBookSourceRepository.findByNoteBookIdAndId(noteBookId, sourceId)
                .orElseThrow(() -> new AppException(ApiResponseStatus.NOTEBOOK_SOURCE_NOT_EXISTS));
    }

    private void validateDownloadableSource(NoteBookSourceEntity source) {
        if (!NoteBookSourceEntity.SourceType.FILE.equals(source.getSourceType())
                || source.getFilePath() == null
                || source.getFilePath().isBlank()) {
            throw new AppException(ApiResponseStatus.NOTEBOOK_SOURCE_NOT_EXISTS);
        }
    }

    private String resolveFileName(NoteBookSourceEntity source) {
        if (source.getDisplayName() != null && !source.getDisplayName().isBlank()) {
            return source.getDisplayName();
        }

        Path filePath = Path.of(source.getFilePath());
        Path fileName = filePath.getFileName();
        return fileName == null ? source.getId().toString() : fileName.toString();
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
