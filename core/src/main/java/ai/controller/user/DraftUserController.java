package ai.controller.user;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
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
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;

import ai.dto.own.request.DraftChatRequestDto;
import ai.dto.own.request.DraftCreateRequestDto;
import ai.dto.own.request.DraftRollbackRequestDto;
import ai.dto.own.request.DraftSaveVersionRequestDto;
import ai.dto.own.request.DraftUpdateRequestDto;
import ai.dto.own.request.MessageFeedbackRequestDto;
import ai.dto.own.request.filter.MessageFilterDto;
import ai.dto.own.response.DraftResponseDto;
import ai.dto.own.response.DraftVersionResponseDto;
import ai.dto.own.response.MessageFeedbackHistoryResponseDto;
import ai.dto.own.response.MessageResponseDto;
import ai.dto.outer.rag.response.RagDraftDocumentTypeDto;
import ai.dto.outer.rag.response.RagDraftFormatStandardDto;
import ai.enums.MessageParentType;
import ai.model.ApiResponseModel;
import ai.model.CustomPairModel;
import ai.service.DraftService;
import ai.service.MessageService;
import ai.service.RagService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import reactor.core.publisher.Flux;

@Tag(name = "Draft", description = "AI drafting APIs with preview and version history")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/user/drafts")
@Tag(name = "Draft", description = "Draft management APIs")
@RestController
public class DraftUserController {
        DraftService draftService;
        MessageService messageService;
        RagService ragService;

        @Operation(summary = "Get draft types", description = "Lấy danh sách loại tài liệu hỗ trợ soạn thảo, quản lý tập trung từ RAG service")
        @GetMapping("/types")
        public ResponseEntity<ApiResponseModel<List<RagDraftDocumentTypeDto>>> types() {
                return ResponseEntity.ok(
                                ApiResponseModel.<List<RagDraftDocumentTypeDto>>builder()
                                                .message("Get draft types successfully")
                                                .data(ragService.getDraftDocumentTypes())
                                                .build());
        }

        @Operation(summary = "Get draft format standards", description = "Lấy danh sách chuẩn định dạng văn bản hỗ trợ, quản lý tập trung từ RAG service")
        @GetMapping("/format-standards")
        public ResponseEntity<ApiResponseModel<List<RagDraftFormatStandardDto>>> formatStandards() {
                return ResponseEntity.ok(
                                ApiResponseModel.<List<RagDraftFormatStandardDto>>builder()
                                                .message("Get draft format standards successfully")
                                                .data(ragService.getDraftFormatStandards())
                                                .build());
        }

        @Operation(summary = "Create draft", description = "Tạo bản nháp mới với nội dung được sinh từ AI")
        @PostMapping
        public ResponseEntity<ApiResponseModel<DraftResponseDto>> create(
                        @Valid @RequestBody DraftCreateRequestDto requestDto) {
                return ResponseEntity.ok(
                                ApiResponseModel.<DraftResponseDto>builder()
                                                .message("Create draft successfully")
                                                .data(draftService.create(requestDto))
                                                .build());
        }

        @Operation(summary = "Update draft", description = "Cập nhật thông tin bản nháp, hiện tại chỉ hỗ trợ cập nhật title")
        @PutMapping("/{draftId}")
        public ResponseEntity<ApiResponseModel<DraftResponseDto>> update(
                        @PathVariable UUID draftId,
                        @Valid @RequestBody DraftUpdateRequestDto requestDto) {
                return ResponseEntity.ok(
                                ApiResponseModel.<DraftResponseDto>builder()
                                                .message("Update draft successfully")
                                                .data(draftService.update(draftId, requestDto))
                                                .build());
        }

        @Operation(summary = "Save new draft version", description = "Lưu thêm một version mới cho draft đã tồn tại, version mới này sẽ được đánh số cao hơn version hiện tại nhất")
        @PostMapping("/{draftId}/versions")
        public ResponseEntity<ApiResponseModel<DraftVersionResponseDto>> saveVersion(
                        @PathVariable UUID draftId,
                        @Valid @RequestBody DraftSaveVersionRequestDto requestDto) {
                return ResponseEntity.ok(
                                ApiResponseModel.<DraftVersionResponseDto>builder()
                                                .message("Save draft version successfully")
                                                .data(draftService.saveVersion(draftId, requestDto))
                                                .build());
        }

        @Operation(summary = "Delete draft version", description = "Xóa một version trong lịch sử soạn thảo của draft, không ảnh hưởng đến version khác")
        @DeleteMapping("/{draftId}/versions/{versionId}")
        public ResponseEntity<ApiResponseModel<Void>> deleteVersion(
                        @PathVariable UUID draftId,
                        @PathVariable UUID versionId) {
                draftService.deleteVersion(draftId, versionId);
                return ResponseEntity.ok(
                                ApiResponseModel.<Void>builder()
                                                .message("Delete draft version successfully")
                                                .build());
        }

        @Operation(summary = "Rollback to an old version", description = "Tạo một version mới từ version cũ để rollback")
        @PostMapping("/{draftId}/versions/{versionId}/rollback")
        public ResponseEntity<ApiResponseModel<DraftVersionResponseDto>> rollbackVersion(
                        @PathVariable UUID draftId,
                        @PathVariable UUID versionId,
                        @Valid @RequestBody DraftRollbackRequestDto requestDto) {
                return ResponseEntity.ok(
                                ApiResponseModel.<DraftVersionResponseDto>builder()
                                                .message("Rollback draft version successfully")
                                                .data(draftService.rollbackToVersion(draftId, versionId,
                                                                requestDto.getReason()))
                                                .build());
        }

        @Operation(summary = "Chat with draft", description = "Gửi yêu cầu chỉnh sửa draft từ nội dung chat, server sẽ gọi AI sinh lại bản draft mới dựa trên lịch sử hội thoại và lưu thành 1 version mới")
        @PostMapping(value = "/{draftId}/messages", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        public Flux<String> chatWithDraft(
                        @PathVariable UUID draftId,
                        @Valid @RequestBody DraftChatRequestDto requestDto) throws JsonProcessingException {
                DraftResponseDto draftResponse = draftService.getById(draftId);
                if(draftResponse == null) {
                        return Flux.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Draft not found"));
                } 

                // Nếu chưa có sessionId thì tạo mới draft, nếu đã có sessionId thì gọi API chatDraft
                if(draftResponse.getSessionId() == null || draftResponse.getSessionId().isEmpty()) {
                        return ragService.draftCreate(draftResponse);
                } 
                // Nếu đã có sessionId thì gọi API chatDraft
                else {
                        return ragService.chatDraft(draftId, requestDto);
                }
        }

        @Operation(summary = "Get draft chat messages", description = "Lấy lịch sử chat draft")
        @GetMapping("/{draftId}/messages")
        public ResponseEntity<ApiResponseModel<List<MessageResponseDto>>> getDraftMessages(
                        @PathVariable UUID draftId,
                        @Valid @ModelAttribute MessageFilterDto filterDto) {
                CustomPairModel<Long, List<MessageResponseDto>> result = messageService.getAll(
                                draftId, MessageParentType.DRAFT, filterDto);
                return ResponseEntity.ok(
                                ApiResponseModel.<List<MessageResponseDto>>builder()
                                                .message("Get draft messages successfully")
                                                .count(result.getFirst())
                                                .data(result.getSecond())
                                                .build());
        }

        @Operation(summary = "Get drafts", description = "Lấy danh sách draft của user")
        @GetMapping
        public ResponseEntity<ApiResponseModel<List<DraftResponseDto>>> getAll(
                        @RequestParam(defaultValue = "0") int pageNumber,
                        @RequestParam(defaultValue = "20") int pageSize) {
                CustomPairModel<Long, List<DraftResponseDto>> result = draftService.getAll(pageNumber, pageSize);

                return ResponseEntity.ok(
                                ApiResponseModel.<List<DraftResponseDto>>builder()
                                                .message("Get drafts successfully")
                                                .count(result.getFirst())
                                                .data(result.getSecond())
                                                .build());
        }

        @Operation(summary = "Get draft detail", description = "Lấy chi tiết draft")
        @GetMapping("/{draftId}")
        public ResponseEntity<ApiResponseModel<DraftResponseDto>> getById(@PathVariable UUID draftId) {
                return ResponseEntity.ok(
                                ApiResponseModel.<DraftResponseDto>builder()
                                                .message("Get draft successfully")
                                                .data(draftService.getById(draftId))
                                                .build());
        }

        @Operation(summary = "Delete draft", description = "Xóa một bản soạn thảo theo id")
        @DeleteMapping("/{draftId}")
        public ResponseEntity<ApiResponseModel<Void>> delete(@PathVariable UUID draftId) {
                draftService.delete(draftId);
                return ResponseEntity.ok(
                                ApiResponseModel.<Void>builder()
                                                .message("Delete draft successfully")
                                                .build());
        }

        @Operation(summary = "Get draft versions", description = "Lấy lịch sử các version của draft")
        @GetMapping("/{draftId}/versions")
        public ResponseEntity<ApiResponseModel<List<DraftVersionResponseDto>>> getVersions(
                        @PathVariable UUID draftId,
                        @RequestParam(defaultValue = "0") int pageNumber,
                        @RequestParam(defaultValue = "20") int pageSize) {
                CustomPairModel<Long, List<DraftVersionResponseDto>> result = draftService.getVersions(draftId,
                                pageNumber, pageSize);

                return ResponseEntity.ok(
                                ApiResponseModel.<List<DraftVersionResponseDto>>builder()
                                                .message("Get draft versions successfully")
                                                .count(result.getFirst())
                                                .data(result.getSecond())
                                                .build());
        }

        @Hidden
        @Operation(summary = "Update draft message feedback", description = "Cập nhật feedback (like/dislike) cho 1 message trong cuộc hội thoại chỉnh sửa draft")
        @PatchMapping("/{draftId}/messages/{messageId}/feedback")
        public ResponseEntity<ApiResponseModel<MessageResponseDto>> updateMessageFeedback(
                        @PathVariable UUID draftId,
                        @PathVariable UUID messageId,
                        @Valid @RequestBody MessageFeedbackRequestDto requestDto) {
                return ResponseEntity.ok(
                                ApiResponseModel.<MessageResponseDto>builder()
                                                .message("Update draft message feedback successfully")
                                                .data(messageService.updateDraftMessageFeedback(
                                                                draftId,
                                                                messageId,
                                                                requestDto.getFeedback()))
                                                .build());
        }

        @Hidden
        @Operation(summary = "Get draft message feedback history", description = "Lấy lịch sử feedback của 1 message trong cuộc hội thoại chỉnh sửa draft")
        @GetMapping("/{draftId}/messages/{messageId}/feedback/history")
        public ResponseEntity<ApiResponseModel<List<MessageFeedbackHistoryResponseDto>>> getMessageFeedbackHistory(
                        @PathVariable UUID draftId,
                        @PathVariable UUID messageId,
                        @RequestParam(defaultValue = "0") int pageNumber,
                        @RequestParam(defaultValue = "20") int pageSize) {
                CustomPairModel<Long, List<MessageFeedbackHistoryResponseDto>> result = messageService
                                .getDraftMessageFeedbackHistory(draftId, messageId, pageNumber, pageSize);
                return ResponseEntity.ok(
                                ApiResponseModel.<List<MessageFeedbackHistoryResponseDto>>builder()
                                                .message("Get draft message feedback history successfully")
                                                .count(result.getFirst())
                                                .data(result.getSecond())
                                                .build());
        }
}
