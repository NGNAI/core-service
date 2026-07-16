package ai.controller.user;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.dto.own.request.dashboard.DashboardUserFilterDto;
import ai.dto.own.response.dashboard.DashboardUserResponseDto;
import ai.model.ApiResponseModel;
import ai.service.dashboard.DashboardUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/user/dashboard")
@Tag(name = "User Dashboard", description = "Dashboard tổng quan cho người dùng")
@RestController
public class DashboardUserController {

    DashboardUserService dashboardUserService;

    @Operation(
        summary = "Get user dashboard",
        description = "Lấy dashboard tổng quan của người dùng hiện tại (lấy userId/orgId từ JWT). " +
                      "Bao gồm: tổng topics, notebooks, notes (nhóm theo loại), data ingestions (theo trạng thái), " +
                      "và xu hướng hoạt động theo ngày (mặc định 7 ngày gần nhất)."
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved user dashboard",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = DashboardUserResponseDto.class)))
    @GetMapping
    public ResponseEntity<ApiResponseModel<DashboardUserResponseDto>> getUserDashboard(
            @Valid @ModelAttribute DashboardUserFilterDto filter) {
        DashboardUserResponseDto dashboard = dashboardUserService.getUserDashboard(filter);
        return ResponseEntity.ok(
                ApiResponseModel.<DashboardUserResponseDto>builder()
                        .message("Lấy dashboard người dùng thành công")
                        .data(dashboard)
                        .build());
    }
}