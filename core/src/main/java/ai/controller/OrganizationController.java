package ai.controller;

import ai.dto.own.request.*;
import ai.dto.own.request.filter.OrganizationFilterDto;
import ai.dto.own.response.OrganizationResponseDto;
import ai.model.ApiResponseModel;
import ai.model.CustomPairModel;
import ai.service.OrganizationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/admin/organizations")
@RestController
public class OrganizationController {
    OrganizationService organizationService;

    @GetMapping("/{organizationId}")
    ResponseEntity<ApiResponseModel<OrganizationResponseDto>> getById(@PathVariable UUID organizationId
            , @Valid @Min(value = 0, message = "NESTED_CHILD_VALUE_INVALID") @RequestParam(required = false) Integer nestedChild){
        return ResponseEntity.ok(
                ApiResponseModel.<OrganizationResponseDto>builder()
                        .message("Get organization successfully")
                        .data(organizationService.getById(organizationId,nestedChild))
                        .build()
        );
    }

    @GetMapping
    ResponseEntity<ApiResponseModel<List<OrganizationResponseDto>>> getAll(@ModelAttribute OrganizationFilterDto filterDto){
        CustomPairModel<Long, List<OrganizationResponseDto>> result = organizationService.getAll(filterDto);
        return ResponseEntity.ok(
                ApiResponseModel.<List<OrganizationResponseDto>>builder()
                        .message("Get list organizations successfully")
                        .count(result.getFirst())
                        .data(result.getSecond())
                        .build()
        );
    }

    @GetMapping("/root")
    ResponseEntity<ApiResponseModel<List<OrganizationResponseDto>>> getRoot(@RequestParam(required = false) Integer nestedChild,@ModelAttribute OrganizationFilterDto filterDto){
        CustomPairModel<Long, List<OrganizationResponseDto>> result = organizationService.getRoot(nestedChild,filterDto);
        return ResponseEntity.ok(
                ApiResponseModel.<List<OrganizationResponseDto>>builder()
                        .message("Get root list organizations successfully")
                        .count(result.getFirst())
                        .data(result.getSecond())
                        .build()
        );
    }

    @GetMapping("/{organizationId}/children")
    ResponseEntity<ApiResponseModel<List<OrganizationResponseDto>>> getChild(@PathVariable UUID organizationId, @RequestParam(required = false) Integer nestedChild,@ModelAttribute OrganizationFilterDto filterDto){
        CustomPairModel<Long, List<OrganizationResponseDto>> result = organizationService.getChild(organizationId, nestedChild, filterDto);

        return ResponseEntity.ok(
                ApiResponseModel.<List<OrganizationResponseDto>>builder()
                        .message("Get children of organizations successfully")
                        .count(result.getFirst())
                        .data(result.getSecond())
                        .build()
        );
    }

    @PostMapping
    ResponseEntity<ApiResponseModel<OrganizationResponseDto>> create(@Valid @RequestBody OrganizationCreateRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<OrganizationResponseDto>builder()
                        .message("Create organization successfully")
                        .data(organizationService.create(requestDto))
                        .build()
        );
    }

    @PutMapping("/{organizationId}")
    ResponseEntity<ApiResponseModel<OrganizationResponseDto>> update(@PathVariable UUID organizationId,@Valid @RequestBody OrganizationUpdateRequestDto requestDto){
        return ResponseEntity.ok(
                ApiResponseModel.<OrganizationResponseDto>builder()
                        .message("Update organization successfully")
                        .data(organizationService.update(organizationId, requestDto))
                        .build()
        );
    }

    @DeleteMapping("/{organizationId}")
    ResponseEntity<ApiResponseModel<Void>> delete(@PathVariable UUID organizationId){
        organizationService.delete(organizationId);

        return ResponseEntity.ok(
                ApiResponseModel.<Void>builder()
                        .message("Delete organization successfully")
                        .build()
        );
    }
}
