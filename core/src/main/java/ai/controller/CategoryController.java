package ai.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.dto.own.response.PermissionActionResponseDto;
import ai.dto.own.response.PermissionResourceResponseDto;
import ai.dto.own.response.PermissionScopeResponseDto;
import ai.entity.postgres.NoteBookSourceEntity;
import ai.enums.DataScope;
import ai.enums.DataSource;
import ai.enums.IngestionStatus;
import ai.enums.MessageFeedbackType;
import ai.enums.NoteSourceBy;
import ai.enums.NoteSourceType;
import ai.model.ApiResponseModel;
import ai.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/category")
@RestController
@Tag(name = "Category", description = "Category management APIs for various enums and constants")
public class CategoryController {
    CategoryService categoryService;

        @Operation(summary = "Get rag scope", description = "Retrieve all RAG scope values")
        @GetMapping("/rag-scope")
        ResponseEntity<ApiResponseModel<List<PermissionResourceResponseDto>>> ragScope() {
        return ResponseEntity.ok(
                ApiResponseModel.<List<PermissionResourceResponseDto>>builder()
                        .message("Get rag scope successfully!")
                        .data(categoryService.getRagScope())
                        .build()
        );
    }

        @Operation(summary = "Get permission resources", description = "Retrieve all permission resource values")
        @GetMapping("/permission-resource")
        ResponseEntity<ApiResponseModel<List<PermissionResourceResponseDto>>> permissionResource() {
        return ResponseEntity.ok(
                ApiResponseModel.<List<PermissionResourceResponseDto>>builder()
                        .message("Get permission resource successfully!")
                        .data(categoryService.getPermissionResource())
                        .build()
        );
    }

        @Operation(summary = "Get permission actions", description = "Retrieve all permission action values")
        @GetMapping("/permission-action")
        ResponseEntity<ApiResponseModel<List<PermissionActionResponseDto>>> permissionAction() {
        return ResponseEntity.ok(
                ApiResponseModel.<List<PermissionActionResponseDto>>builder()
                        .message("Get permission action successfully!")
                        .data(categoryService.getPermissionAction())
                        .build()
        );
    }

        @Operation(summary = "Get permission scopes", description = "Retrieve all permission scope values")
        @GetMapping("/permission-scope")
        ResponseEntity<ApiResponseModel<List<PermissionScopeResponseDto>>> permissionScope() {
        return ResponseEntity.ok(
                ApiResponseModel.<List<PermissionScopeResponseDto>>builder()
                        .message("Get permission scope category successfully")
                        .data(categoryService.getPermissionScope())
                        .build()
        );
    }

        @Operation(summary = "Get notebook source types", description = "Retrieve all notebook source type values")
        @GetMapping("/source-types-of-notebook")
        ResponseEntity<ApiResponseModel<List<NoteBookSourceEntity.SourceType>>> sourceTypesOfNotebook() {
        return ResponseEntity.ok(
                ApiResponseModel.<List<NoteBookSourceEntity.SourceType>>builder()
                        .message("Get source types successfully")
                        .data(Arrays.asList(NoteBookSourceEntity.SourceType.values()))
                        .build());
    }

        @Operation(summary = "Get notebook vector statuses", description = "Retrieve all notebook vector status values")
        @GetMapping("/vector-statuses-of-notebook-source")
        ResponseEntity<ApiResponseModel<List<NoteBookSourceEntity.VectorStatus>>> vectorStatusesOfNotebookSource() {
        return ResponseEntity.ok(
                ApiResponseModel.<List<NoteBookSourceEntity.VectorStatus>>builder()
                        .message("Get vector statuses successfully")
                        .data(Arrays.asList(NoteBookSourceEntity.VectorStatus.values()))
                        .build());
    }

        @Operation(summary = "Get message feedback types", description = "Retrieve all message feedback type values")
        @GetMapping("/message-feedback-types")
        ResponseEntity<ApiResponseModel<List<String>>> messageFeedbackTypes() {
        List<String> feedbackTypes = Arrays.asList(MessageFeedbackType.values()).stream()
                .map(MessageFeedbackType::getValue)
                .toList();

        return ResponseEntity.ok(
                ApiResponseModel.<List<String>>builder()
                        .message("Get message feedback types successfully")
                        .data(feedbackTypes)
                        .build());
    }

        @Operation(summary = "Get note source types", description = "Retrieve all note source type values")
        @GetMapping("/source-types-of-note")
        ResponseEntity<ApiResponseModel<List<NoteSourceType>>> sourceTypesOfNote() {
        return ResponseEntity.ok(
                ApiResponseModel.<List<NoteSourceType>>builder()
                        .message("Get note source types successfully")
                        .data(Arrays.asList(NoteSourceType.values()))
                        .build());
    }

        @Operation(summary = "Get note source by values", description = "Retrieve all note source by values")
        @GetMapping("/source-by-of-note")
        ResponseEntity<ApiResponseModel<List<NoteSourceBy>>> sourceByOfNote() {
        return ResponseEntity.ok(
                ApiResponseModel.<List<NoteSourceBy>>builder()
                        .message("Get note source by successfully")
                        .data(Arrays.asList(NoteSourceBy.values()))
                        .build());
    }

        @Operation(summary = "Get data ingestion access levels", description = "Retrieve all data ingestion access level values")
        @GetMapping("/level-of-data-ingestion-access")
        ResponseEntity<ApiResponseModel<List<DataScope>>> levelOfDataIngestionAccess() {
                return ResponseEntity.ok(
                                ApiResponseModel.<List<DataScope>>builder()
                                                .message("Get data ingestion access levels successfully")
                                                .data(Arrays.asList(DataScope.values()))
                                                .build());
        }

        @Operation(summary = "Get data ingestion sources", description = "Retrieve all data ingestion source values")
        @GetMapping("/source-of-data-ingestion")
        ResponseEntity<ApiResponseModel<List<DataSource>>> sourceOfDataIngestion() {
                return ResponseEntity.ok(
                                ApiResponseModel.<List<DataSource>>builder()
                                                .message("Get data ingestion sources successfully")
                                                .data(Arrays.asList(DataSource.values()))
                                                .build());
        }

        @Operation(summary = "Get data ingestion statuses", description = "Retrieve all data ingestion status values")
        @GetMapping("/ingestion-status-of-data-ingestion")
        ResponseEntity<ApiResponseModel<List<String>>> ingestionStatusOfDataIngestion() {
                List<String> ingestionStatuses = Arrays.asList(IngestionStatus.values()).stream()
                                .map(Enum::name)
                                .toList();
                return ResponseEntity.ok(
                                ApiResponseModel.<List<String>>builder()
                                                .message("Get data ingestion statuses successfully")
                                                .data(ingestionStatuses)
                                                .build());
        }      
}
