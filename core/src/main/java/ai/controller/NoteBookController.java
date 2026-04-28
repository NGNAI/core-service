package ai.controller;

import java.util.List;
import java.util.UUID;

import ai.dto.own.request.*;
import ai.dto.outer.ingestion.response.IngestionStatusResponseDto;
import ai.enums.MessageParentType;
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
import ai.dto.own.response.MessageResponseDto;
import ai.dto.own.response.NoteBookSourceDownloadData;
import ai.dto.own.response.NoteBookSourceJobStatusResponseDto;
import ai.dto.own.response.NoteBookSourcePresignedUrlResponseDto;
import ai.dto.own.response.NoteBookSourceResponseDto;
import ai.dto.own.response.NoteBookResponseDto;
import ai.model.ApiResponseModel;
import ai.model.CustomPairModel;
import ai.service.MessageService;
import ai.service.NoteBookSourceService;
import ai.service.NoteBookService;
import ai.service.RagService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import reactor.core.publisher.Flux;

@Tag(name = "NoteBook", description = "Notebook management APIs including sources and notebook chat")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/user/notebooks")
@RestController
public class NoteBookController {
    NoteBookService notebookService;
    MessageService messageService;
    NoteBookSourceService noteBookSourceService;
    RagService ragService;

    @Operation(summary = "Get notebooks", description = "Lấy danh sách notebook của người dùng với phân trang và bộ lọc")
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

    @Operation(summary = "Create notebook", description = "Tạo mới một notebook")
    @PostMapping()
    ResponseEntity<ApiResponseModel<NoteBookResponseDto>> create(@Valid @RequestBody NoteBookCreateRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<NoteBookResponseDto>builder()
                        .message("Create notebook successfully")
                        .data(notebookService.create(requestDto))
                        .build()
        );
    }

    @Operation(summary = "Get notebook detail", description = "Lấy chi tiết một notebook theo id")
    @GetMapping("/{noteBookId}")
    ResponseEntity<ApiResponseModel<NoteBookResponseDto>> getById(@PathVariable UUID noteBookId){
        return ResponseEntity.ok(
                ApiResponseModel.<NoteBookResponseDto>builder()
                        .message("Get notebook successfully")
                        .data(notebookService.getById(noteBookId))
                        .build()
        );
    }

    @Operation(summary = "Update notebook", description = "Cập nhật thông tin notebook gồm title, description và instruction")
    @PutMapping("/{noteBookId}")
    ResponseEntity<ApiResponseModel<NoteBookResponseDto>> update(@PathVariable UUID noteBookId, @Valid @RequestBody NoteBookUpdateRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<NoteBookResponseDto>builder()
                        .message("Update notebook successfully")
                        .data(notebookService.update(noteBookId, requestDto))
                        .build()
        );
    }

    @Operation(summary = "Rename notebook title", description = "Cập nhật riêng tiêu đề của notebook")
    @PatchMapping("/{noteBookId}/title")
    ResponseEntity<ApiResponseModel<NoteBookResponseDto>> renameTitle(@PathVariable UUID noteBookId, @Valid @RequestBody NoteBookRenameTitleRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<NoteBookResponseDto>builder()
                        .message("Rename notebook title successfully")
                        .data(notebookService.renameTitle(noteBookId, requestDto))
                        .build()
        );
    }

    @Operation(summary = "Update notebook instruction", description = "Cập nhật riêng instruction của notebook")
    @PatchMapping("/{noteBookId}/instruction")
    ResponseEntity<ApiResponseModel<NoteBookResponseDto>> updateInstruction(@PathVariable UUID noteBookId, @Valid @RequestBody NoteBookUpdateInstructionRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<NoteBookResponseDto>builder()
                        .message("Update notebook instruction successfully")
                        .data(notebookService.updateInstruction(noteBookId, requestDto))
                        .build()
        );
    }

    @Operation(summary = "Delete notebook", description = "Xóa một notebook theo id")
    @DeleteMapping("/{noteBookId}")
    ResponseEntity<ApiResponseModel<Void>> delete(@PathVariable UUID noteBookId){
        notebookService.delete(noteBookId);

        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Delete notebook successfully")
                        .build()
        );
    }

        @Operation(summary = "Get notebook sources", description = "Lấy danh sách nguồn dữ liệu đã gắn vào notebook")
        @GetMapping("/{noteBookId}/sources")
        ResponseEntity<ApiResponseModel<List<NoteBookSourceResponseDto>>> getSources(@PathVariable UUID noteBookId,
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "20") int pageSize) {
                CustomPairModel<Long, List<NoteBookSourceResponseDto>> result = noteBookSourceService.getSources(noteBookId, pageNumber, pageSize);
        return ResponseEntity.ok(
                ApiResponseModel.<List<NoteBookSourceResponseDto>>builder()
                                                .message("Get list notebook sources successfully")
                        .count(result.getFirst())
                        .data(result.getSecond())
                        .build()
        );
    }

    @Hidden
        @Operation(summary = "Download notebook source", description = "Tải file nguồn của notebook khi source là FILE")
        @GetMapping("/{noteBookId}/sources/{sourceId}/download")
        ResponseEntity<byte[]> downloadSource(@PathVariable UUID noteBookId, @PathVariable UUID sourceId) {
                NoteBookSourceDownloadData fileData = noteBookSourceService.downloadSource(noteBookId, sourceId);

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

    @Hidden
    @Operation(summary = "Get notebook source download URL", description = "Lấy presigned URL để tải file nguồn của notebook")
    @GetMapping("/{noteBookId}/sources/{sourceId}/download-url")
    ResponseEntity<ApiResponseModel<NoteBookSourcePresignedUrlResponseDto>> getDownloadUrl(
        @PathVariable UUID noteBookId, 
        @PathVariable UUID sourceId,
        @Parameter(description = "URL expiration in seconds, default 900", example = "900") @RequestParam(required = false) Integer expiresInSeconds ) {
        NoteBookSourcePresignedUrlResponseDto downloadUrl = noteBookSourceService.getSourceDownloadUrl(noteBookId, sourceId, expiresInSeconds);
        return ResponseEntity.ok(
                ApiResponseModel.<NoteBookSourcePresignedUrlResponseDto>builder()
                        .message("Get source download URL successfully")
                        .data(downloadUrl)
                        .build()
        );
    }

        @Operation(summary = "Add notebook file sources", description = "Thêm một hoặc nhiều nguồn FILE vào notebook từ file upload")
        @PostMapping(value = "/{noteBookId}/sources/add-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        ResponseEntity<ApiResponseModel<List<NoteBookSourceResponseDto>>> addFileSources(@PathVariable UUID noteBookId,
            @Valid @ModelAttribute NoteBookSourceAddFilesRequestDto requestDto) {
        return ResponseEntity.ok(
                ApiResponseModel.<List<NoteBookSourceResponseDto>>builder()
                .message("Add file sources to notebook successfully")
            .data(noteBookSourceService.addFileSources(noteBookId, requestDto))
                .build()
        );
        }

        @Operation(summary = "Add notebook text source", description = "Thêm một nguồn TEXT vào notebook từ nội dung text thô")
        @PostMapping(value = "/{noteBookId}/sources/add-text")
        ResponseEntity<ApiResponseModel<NoteBookSourceResponseDto>> addTextSource(@PathVariable UUID noteBookId,
            @Valid @RequestBody NoteBookSourceAddTextRequestDto requestDto) {
        return ResponseEntity.ok(
            ApiResponseModel.<NoteBookSourceResponseDto>builder()
                .message("Add text source to notebook successfully")
                .data(noteBookSourceService.addTextSource(noteBookId, requestDto))
                .build()
        );
        }

        @Operation(summary = "Add notebook note sources", description = "Thêm một hoặc nhiều nguồn NOTE vào notebook từ các note đã tồn tại")
        @PostMapping(value = "/{noteBookId}/sources/add-notes")
        ResponseEntity<ApiResponseModel<List<NoteBookSourceResponseDto>>> addNoteSources(@PathVariable UUID noteBookId,
            @Valid @RequestBody NoteBookSourceAddNotesRequestDto requestDto) {
        return ResponseEntity.ok(
            ApiResponseModel.<List<NoteBookSourceResponseDto>>builder()
                .message("Add note sources to notebook successfully")
                .data(noteBookSourceService.addNoteSources(noteBookId, requestDto))
                        .build()
        );
    }

    @Operation(summary = "Remove notebook source", description = "Gỡ một nguồn dữ liệu khỏi notebook")
    @DeleteMapping("/{noteBookId}/sources/{sourceId}")
    ResponseEntity<ApiResponseModel<Void>> removeSource(@PathVariable UUID noteBookId, @PathVariable UUID sourceId) {
        noteBookSourceService.removeSource(noteBookId, sourceId);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Remove source from notebook successfully")
                        .build()
        );
    }

    @Operation(summary = "Get notebook source ingestion job status", description = "Poll trạng thái embedding của notebook source")
    @GetMapping("/{noteBookId}/sources/{sourceId}/ingestion/job-status")
    ResponseEntity<ApiResponseModel<NoteBookSourceJobStatusResponseDto>> ingestionJobStatus(
            @PathVariable UUID noteBookId,
            @PathVariable UUID sourceId) {
        return ResponseEntity.ok(
                ApiResponseModel.<NoteBookSourceJobStatusResponseDto>builder()
                        .message("Get notebook source job status successfully")
                    .data(noteBookSourceService.pollIngestionJobStatus(noteBookId, sourceId))
                        .build()
        );
    }

    @Operation(summary = "Receive notebook source ingestion callback status", description = "Webhook endpoint để ingestion service callback trạng thái embedding notebook source")
    @PostMapping(value = "/sources/ingestion/webhook/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApiResponseModel<NoteBookSourceJobStatusResponseDto>> ingestionWebhookStatus(
            @RequestBody IngestionStatusResponseDto callbackPayload) {
        return ResponseEntity.ok(
                ApiResponseModel.<NoteBookSourceJobStatusResponseDto>builder()
                        .message("Receive notebook source ingestion callback successfully")
                    .data(noteBookSourceService.handleIngestionCallback(callbackPayload))
                        .build()
        );
    }

    @Operation(summary = "Get notebook messages", description = "Lấy lịch sử message của notebook")
    @GetMapping("/{noteBookId}/messages")
    ResponseEntity<ApiResponseModel<List<MessageResponseDto>>> getMessageByNoteBookId(@PathVariable UUID noteBookId,@Valid @ModelAttribute MessageFilterDto filterDto){
        CustomPairModel<Long, List<MessageResponseDto>> result = messageService.getAll(noteBookId, MessageParentType.NOTEBOOK, filterDto);
        return ResponseEntity.ok(
                ApiResponseModel.<List<MessageResponseDto>>builder()
                        .message("Get list message of notebook successfully")
                        .count(result.getFirst())
                        .data(result.getSecond())
                        .build()
        );
    }

    @Operation(summary = "Chat with notebook", description = "Gửi câu hỏi vào notebook và nhận phản hồi dạng SSE stream")
    @PostMapping(value = "/{noteBookId}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> postMessageByNoteBookIdFlux(
            @PathVariable UUID noteBookId,
            @Valid @ModelAttribute NoteBookCreateConversationRequestDto requestDto) throws JsonProcessingException {
        return ragService.chatNoteBook(noteBookId, requestDto);
    }
}
