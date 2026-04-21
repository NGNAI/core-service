package ai.controller;

import java.util.List;
import java.util.UUID;

import ai.dto.own.request.*;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;

import ai.dto.own.request.filter.MessageFilterDto;
import ai.dto.own.request.filter.NoteBookFilterDto;
import ai.dto.own.response.DataIngestionDownloadData;
import ai.dto.own.response.DataIngestionPresignedUrlResponseDto;
import ai.dto.own.response.MessageResponseDto;
import ai.dto.own.response.NoteBookFileResponseDto;
import ai.dto.own.response.NoteBookResponseDto;
import ai.model.ApiResponseModel;
import ai.model.CustomPairModel;
import ai.service.MessageService;
import ai.service.NoteBookFileService;
import ai.service.NoteBookService;
import ai.service.RagService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/user/notebooks")
@RestController
public class NoteBookController {
    NoteBookService notebookService;
    MessageService messageService;
    NoteBookFileService noteBookFileService;
    RagService ragService;

    @GetMapping()
    ResponseEntity<ApiResponseModel<List<NoteBookResponseDto>>> getAllByUserId(@Valid @ModelAttribute NoteBookFilterDto filterDto){
        CustomPairModel<Long, List<NoteBookResponseDto>> result = notebookService.getAll(filterDto);
        return ResponseEntity.ok(
                ApiResponseModel.<List<NoteBookResponseDto>>builder()
                        .message("Get list notebooks successfully")
                        .count(result.getFirst())
                        .data(result.getSecond())
                        .build()
        );
    }

    @PostMapping()
    ResponseEntity<ApiResponseModel<NoteBookResponseDto>> create(@Valid @RequestBody NoteBookCreateRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<NoteBookResponseDto>builder()
                        .message("Create notebook successfully")
                        .data(notebookService.create(requestDto))
                        .build()
        );
    }

    @GetMapping("/{noteBookId}")
    ResponseEntity<ApiResponseModel<NoteBookResponseDto>> getById(@PathVariable UUID noteBookId){
        return ResponseEntity.ok(
                ApiResponseModel.<NoteBookResponseDto>builder()
                        .message("Get notebook successfully")
                        .data(notebookService.getById(noteBookId))
                        .build()
        );
    }

    @PutMapping("/{noteBookId}")
    ResponseEntity<ApiResponseModel<NoteBookResponseDto>> update(@PathVariable UUID noteBookId, @Valid @RequestBody NoteBookUpdateRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<NoteBookResponseDto>builder()
                        .message("Update notebook successfully")
                        .data(notebookService.update(noteBookId, requestDto))
                        .build()
        );
    }

    @PatchMapping("/{noteBookId}/title")
    ResponseEntity<ApiResponseModel<NoteBookResponseDto>> renameTitle(@PathVariable UUID noteBookId, @Valid @RequestBody NoteBookRenameTitleRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<NoteBookResponseDto>builder()
                        .message("Rename notebook title successfully")
                        .data(notebookService.renameTitle(noteBookId, requestDto))
                        .build()
        );
    }

    @PatchMapping("/{noteBookId}/instruction")
    ResponseEntity<ApiResponseModel<NoteBookResponseDto>> updateInstruction(@PathVariable UUID noteBookId, @Valid @RequestBody NoteBookUpdateInstructionRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<NoteBookResponseDto>builder()
                        .message("Update notebook instruction successfully")
                        .data(notebookService.updateInstruction(noteBookId, requestDto))
                        .build()
        );
    }

    @DeleteMapping("/{noteBookId}")
    ResponseEntity<ApiResponseModel<Void>> delete(@PathVariable UUID noteBookId){
        notebookService.delete(noteBookId);

        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Delete notebook successfully")
                        .build()
        );
    }

    @GetMapping("/{noteBookId}/files")
    ResponseEntity<ApiResponseModel<List<NoteBookFileResponseDto>>> getFiles(@PathVariable UUID noteBookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        CustomPairModel<Long, List<NoteBookFileResponseDto>> result = noteBookFileService.getFiles(noteBookId, page, size);
        return ResponseEntity.ok(
                ApiResponseModel.<List<NoteBookFileResponseDto>>builder()
                        .message("Get list notebook files successfully")
                        .count(result.getFirst())
                        .data(result.getSecond())
                        .build()
        );
    }

    @GetMapping("/{noteBookId}/files/{fileId}/download")
    ResponseEntity<byte[]> downloadFile(@PathVariable UUID noteBookId, @PathVariable UUID fileId) {
        DataIngestionDownloadData fileData = noteBookFileService.downloadFile(noteBookId, fileId);

        HttpHeaders headers = new HttpHeaders();
                String contentType = (fileData.contentType() == null || fileData.contentType().isBlank())
                                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                                : fileData.contentType();
                headers.setContentType(MediaType.parseMediaType(contentType));
                headers.setContentDisposition(ContentDisposition.attachment().filename(fileData.fileName()).build());

                return ResponseEntity.ok()
                                .headers(headers)
                                .body(fileData.bytes());
    }

    @GetMapping("/{noteBookId}/files/{fileId}/download-url")
    ResponseEntity<ApiResponseModel<DataIngestionPresignedUrlResponseDto>> getDownloadUrl(
        @PathVariable UUID noteBookId, 
        @PathVariable UUID fileId,
        @Parameter(description = "URL expiration in seconds, default 900", example = "900") @RequestParam(required = false) Integer expiresInSeconds ) {
        DataIngestionPresignedUrlResponseDto downloadUrl = noteBookFileService.getDownloadUrl(noteBookId, fileId, expiresInSeconds);
        return ResponseEntity.ok(
                ApiResponseModel.<DataIngestionPresignedUrlResponseDto>builder()
                        .message("Get file download URL successfully")
                        .data(downloadUrl)
                        .build()
        );
    }

    @PostMapping(value = "/{noteBookId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<ApiResponseModel<List<NoteBookFileResponseDto>>> addFiles(@PathVariable UUID noteBookId,
            @Valid @ModelAttribute NoteBookFilesAddRequestDto requestDto) {
        return ResponseEntity.ok(
                ApiResponseModel.<List<NoteBookFileResponseDto>>builder()
                        .message("Add file to notebook successfully")
                .data(noteBookFileService.uploadFilesAndWaitForCompletion(noteBookId, requestDto.getFiles()))
                        .build()
        );
    }

    @DeleteMapping("/{noteBookId}/files/{fileId}")
    ResponseEntity<ApiResponseModel<Void>> removeFile(@PathVariable UUID noteBookId, @PathVariable UUID fileId) {
        noteBookFileService.removeFile(noteBookId, fileId);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Remove file from notebook successfully")
                        .build()
        );
    }

    @GetMapping("/{noteBookId}/messages")
    ResponseEntity<ApiResponseModel<List<MessageResponseDto>>> getMessageByNoteBookId(@PathVariable UUID noteBookId,@Valid @ModelAttribute MessageFilterDto filterDto){
        CustomPairModel<Long, List<MessageResponseDto>> result = messageService.getAll(noteBookId, filterDto);
        return ResponseEntity.ok(
                ApiResponseModel.<List<MessageResponseDto>>builder()
                        .message("Get list message of notebook successfully")
                        .count(result.getFirst())
                        .data(result.getSecond())
                        .build()
        );
    }

    @PostMapping(value = "/{noteBookId}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> postMessageByNoteBookIdFlux(
            @PathVariable UUID noteBookId,
            @Valid @ModelAttribute NoteBookCreateConversationRequestDto requestDto) throws JsonProcessingException {
        return ragService.chatNoteBook(noteBookId, requestDto);
    }
}
