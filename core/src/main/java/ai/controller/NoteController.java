package ai.controller;

import ai.dto.own.request.NoteCreateRequestDto;
import ai.dto.own.request.NoteUpdateRequestDto;
import ai.dto.own.request.filter.NoteFilterDto;
import ai.dto.own.response.NoteResponseDto;
import ai.model.ApiResponseModel;
import ai.model.CustomPairModel;
import ai.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Note", description = "Note management APIs for topic and notebook sources")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/user/notes")
@RestController
public class NoteController {
    NoteService noteService;

        @Operation(summary = "Get notes", description = "Lấy danh sách ghi chú của người dùng với các bộ lọc theo source type, topicId, notebookId và keyword")
    @GetMapping
    ResponseEntity<ApiResponseModel<List<NoteResponseDto>>> getAll(@Valid @ModelAttribute NoteFilterDto filterDto) {
        CustomPairModel<Long, List<NoteResponseDto>> result = noteService.getAll(filterDto);
        return ResponseEntity.ok(
                ApiResponseModel.<List<NoteResponseDto>>builder()
                        .message("Get list notes successfully")
                        .count(result.getFirst())
                        .data(result.getSecond())
                        .build());
    }

        @Operation(summary = "Create note", description = "Tạo ghi chú mới cho một chat topic hoặc notebook cụ thể")
    @PostMapping
    ResponseEntity<ApiResponseModel<NoteResponseDto>> create(@Valid @RequestBody NoteCreateRequestDto requestDto) {
        return ResponseEntity.ok(
                ApiResponseModel.<NoteResponseDto>builder()
                        .message("Create note successfully")
                        .data(noteService.create(requestDto))
                        .build());
    }

        @Operation(summary = "Get note detail", description = "Lấy chi tiết một ghi chú theo id")
    @GetMapping("/{noteId}")
    ResponseEntity<ApiResponseModel<NoteResponseDto>> getById(@PathVariable UUID noteId) {
        return ResponseEntity.ok(
                ApiResponseModel.<NoteResponseDto>builder()
                        .message("Get note successfully")
                        .data(noteService.getById(noteId))
                        .build());
    }

        @Operation(summary = "Update note", description = "Cập nhật nội dung và nguồn liên kết của một ghi chú")
    @PutMapping("/{noteId}")
    ResponseEntity<ApiResponseModel<NoteResponseDto>> update(@PathVariable UUID noteId,
            @Valid @RequestBody NoteUpdateRequestDto requestDto) {
        return ResponseEntity.ok(
                ApiResponseModel.<NoteResponseDto>builder()
                        .message("Update note successfully")
                        .data(noteService.update(noteId, requestDto))
                        .build());
    }

        @Operation(summary = "Delete note", description = "Xóa một ghi chú theo id")
    @DeleteMapping("/{noteId}")
    ResponseEntity<ApiResponseModel<Void>> delete(@PathVariable UUID noteId) {
        noteService.delete(noteId);
        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Delete note successfully")
                        .build());
    }
}
