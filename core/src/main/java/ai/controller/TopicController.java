package ai.controller;

import java.util.List;
import java.util.UUID;

import ai.enums.MessageParentType;
import org.springframework.data.util.Pair;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;

import ai.dto.own.request.TopicCreateConversationRequestDto;
import ai.dto.own.request.TopicCreateRequestDto;
import ai.dto.own.request.TopicRenameTitleRequestDto;
import ai.dto.own.request.filter.MessageFilterDto;
import ai.dto.own.request.filter.TopicFilterDto;
import ai.dto.own.response.MessageResponseDto;
import ai.dto.own.response.TopicFileResponseDto;
import ai.dto.own.response.TopicResponseDto;
import ai.model.ApiResponseModel;
import ai.model.CustomPairModel;
import ai.service.MessageService;
import ai.service.RagService;
import ai.service.TopicFileService;
import ai.service.TopicService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/user/topics")
@RestController
public class TopicController {
    TopicService topicService;
    MessageService messageService;
    TopicFileService topicFileService;
    RagService ragService;

    @GetMapping()
    ResponseEntity<ApiResponseModel<List<TopicResponseDto>>> getAllByUserId(@Valid @ModelAttribute TopicFilterDto filterDto){
        CustomPairModel<Long, List<TopicResponseDto>> result = topicService.getAll(filterDto);
        return ResponseEntity.ok(
                ApiResponseModel.<List<TopicResponseDto>>builder()
                        .message("Get list topics successfully")
                        .count(result.getFirst())
                        .data(result.getSecond())
                        .build()
        );
    }

    @PostMapping()
    ResponseEntity<ApiResponseModel<TopicResponseDto>> create(@Valid @RequestBody TopicCreateRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<TopicResponseDto>builder()
                        .message("Create topic successfully")
                        .data(topicService.create(requestDto))
                        .build()
        );
    }

    @PatchMapping("/{topicId}")
    ResponseEntity<ApiResponseModel<TopicResponseDto>> renameTitle(@PathVariable UUID topicId, @Valid @RequestBody TopicRenameTitleRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<TopicResponseDto>builder()
                        .message("Rename topic title successfully")
                        .data(topicService.renameTitle(topicId, requestDto))
                        .build()
        );
    }

    @DeleteMapping("/{topicId}")
    ResponseEntity<ApiResponseModel<Void>> delete(@PathVariable UUID topicId){
        topicService.delete(topicId);

        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Delete topic successfully")
                        .build()
        );
    }

    @GetMapping("/{topicId}/files")
    ResponseEntity<ApiResponseModel<List<TopicFileResponseDto>>> getFiles(@PathVariable UUID topicId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pair<Long, List<TopicFileResponseDto>> result = topicFileService.getFiles(topicId, page, size);
        return ResponseEntity.ok(
                ApiResponseModel.<List<TopicFileResponseDto>>builder()
                        .message("Get list topic files successfully")
                        .count(result.getFirst())
                        .data(result.getSecond())
                        .build()
        );
    }

    @GetMapping("/{topicId}/messages")
    ResponseEntity<ApiResponseModel<List<MessageResponseDto>>> getMessageByTopicId(@PathVariable UUID topicId,@Valid @ModelAttribute MessageFilterDto filterDto){
        CustomPairModel<Long, List<MessageResponseDto>> result = messageService.getAll(topicId, MessageParentType.TOPIC, filterDto);
        return ResponseEntity.ok(
                ApiResponseModel.<List<MessageResponseDto>>builder()
                        .message("Get list message of topic successfully")
                        .count(result.getFirst())
                        .data(result.getSecond())
                        .build()
        );
    }

    @PostMapping(value = "/{topicId}/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> postMessageByTopicIdFlux(
            @PathVariable UUID topicId,
            @Valid @ModelAttribute TopicCreateConversationRequestDto requestDto) throws JsonProcessingException {
        
        // Nếu có file đính kèm thì sẽ upload file lên MinIO trước để đảm bảo nếu có lỗi xảy ra khi upload file thì sẽ không tạo bản ghi topic file trong database, tránh trường hợp dữ liệu bị lỗi không thể retry được, sau khi upload file xong sẽ tạo bản ghi topic file mới trong database với thông tin về file đã upload và trạng thái ingestion là PENDING, sau đó gọi API của ingestion service để đẩy file đã upload sang ingestion service để xử lý, nếu có lỗi xảy ra khi gọi API của ingestion service hoặc response trả về không hợp lệ thì sẽ cập nhật trạng thái ingestion của topic file này thành FAILED để tránh bị treo ở trạng thái PENDING mãi mãi, đồng thời trả về response cho client để client có thể hiển thị thông báo lỗi chính xác
        if (requestDto.getFiles() != null && requestDto.getFiles().length > 0) {
            List<TopicFileResponseDto> uploadedFiles = topicFileService.uploadFilesAndWaitForCompletion(topicId, requestDto.getFiles());
            uploadedFiles.forEach(uploadedFile -> {
                MessageResponseDto attachmentMessage = messageService.createAttachmentMessage(topicId, uploadedFile.getDataIngestion());
                topicFileService.updateMessageId(uploadedFile.getId(), attachmentMessage.getId());
            });
        }

        return ragService.chatTopic(topicId, requestDto);
    }
}
