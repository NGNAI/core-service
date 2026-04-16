package ai.controller;

import ai.dto.own.request.NoteBookCreateRequestDto;
import ai.dto.own.request.NoteBookRenameTitleRequestDto;
import ai.dto.own.request.filter.MessageFilterDto;
import ai.dto.own.request.filter.NoteBookFilterDto;
import ai.dto.own.response.MessageResponseDto;
import ai.dto.own.response.NoteBookResponseDto;
import ai.model.ApiResponseModel;
import ai.model.CustomPairModel;
import ai.service.MessageService;
import ai.service.NoteBookService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/user/notebooks")
@RestController
public class NoteBookController {
    NoteBookService notebookService;
    MessageService messageService;

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

    @PatchMapping("/{noteBookId}")
    ResponseEntity<ApiResponseModel<NoteBookResponseDto>> renameTitle(@PathVariable UUID noteBookId, @Valid @RequestBody NoteBookRenameTitleRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<NoteBookResponseDto>builder()
                        .message("Rename notebook title successfully")
                        .data(notebookService.renameTitle(noteBookId, requestDto))
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
}
