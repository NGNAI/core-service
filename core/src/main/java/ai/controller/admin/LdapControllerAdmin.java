package ai.controller.admin;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ai.dto.own.request.ldap.LdapImportRequestDto;
import ai.dto.own.response.ldap.LdapImportResponseDto;
import ai.dto.own.response.ldap.LdapUserResponseDto;
import ai.model.ApiResponseModel;
import ai.service.LdapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/admin/ldap")
@Tag(name = "LDAP Admin", description = "Quản lý user LDAP: search, import, xem chi tiết")
@RestController
public class LdapControllerAdmin {
    LdapService ldapService;

    @Operation(summary = "Tìm kiếm user LDAP", description = "Tìm kiếm user trong LDAP qua OTP Service. Trả về danh sách kèm trạng thái đã import trong hệ thống chưa.")
    @GetMapping("/users/search")
    @PreAuthorize("@perm.canAccess(null, 'USER', 'READ', null)")
    ResponseEntity<ApiResponseModel<List<LdapUserResponseDto>>> searchLdapUsers(
            @RequestParam(required = false, defaultValue = "") String keyword) {
        List<LdapUserResponseDto> users = ldapService.searchLdapUsers(keyword);
        return ResponseEntity.ok(
                ApiResponseModel.<List<LdapUserResponseDto>>builder()
                        .message("Search LDAP users successfully")
                        .count((long) users.size())
                        .data(users)
                        .build()
        );
    }

    @Operation(summary = "Xem chi tiết user LDAP", description = "Lấy thông tin chi tiết 1 user LDAP theo userId. Kèm trạng thái đã import trong hệ thống chưa.")
    @GetMapping("/users/{ldapUserId}")
    @PreAuthorize("@perm.canAccess(null, 'USER', 'READ', null)")
    ResponseEntity<ApiResponseModel<LdapUserResponseDto>> getLdapUserDetail(@PathVariable String ldapUserId) {
        return ResponseEntity.ok(
                ApiResponseModel.<LdapUserResponseDto>builder()
                        .message("Get LDAP user detail successfully")
                        .data(ldapService.getLdapUserDetail(ldapUserId))
                        .build()
        );
    }

    @Operation(summary = "Import user LDAP vào hệ thống", description = "Import một loạt user LDAP vào organization. Mỗi user được xử lý độc lập (partial success). Role có thể để null → fallback về role mặc định.")
    @PostMapping("/users/import")
    @PreAuthorize("@perm.canAccess(#request.organizationId, 'ORG', 'ASSIGN', 'USER')")
    ResponseEntity<ApiResponseModel<LdapImportResponseDto>> importLdapUsers(@Valid @RequestBody LdapImportRequestDto request) {
        return ResponseEntity.ok(
                ApiResponseModel.<LdapImportResponseDto>builder()
                        .message("Import LDAP users completed")
                        .data(ldapService.importLdapUsers(request))
                        .build()
        );
    }
}