package ai.service.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ai.dto.own.request.report.UserReportFilterDto;
import ai.dto.own.response.report.UserReportResponseDto;
import ai.dto.own.response.report.UserReportResponseDto.OrgUserSummaryDto;
import ai.dto.own.response.report.UserReportResponseDto.RoleUserSummaryDto;
import ai.entity.postgres.OrganizationEntity;
import ai.enums.ApiResponseStatus;
import ai.exception.AppException;
import ai.repository.OrganizationRepository;
import ai.repository.OrganizationUserRoleRepository;
import ai.service.OrganizationService;
import ai.util.JwtUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserReportService {

    OrganizationRepository orgRepository;
    OrganizationUserRoleRepository ourRepository;
    OrganizationService organizationService;

    /**
     * Lấy báo cáo tổng quan người dùng theo đơn vị (và các đơn vị con nếu có).
     * 
     * @param filter Bộ lọc: orgId, includeDescendants, keyword, active, source
     * @return UserReportResponseDto chứa thống kê tổng quan, theo đơn vị và theo vai trò
     */
    public UserReportResponseDto getUserReport(UserReportFilterDto filter) {
        // 1. Xác định orgId từ JWT token
        UUID orgId = JwtUtil.getOrgId();
        if (orgId == null) {
            throw new AppException(ApiResponseStatus.ORG_ID_REQUIRED);
        }

        // 2. Lấy danh sách org IDs (chính nó + con nếu includeDescendants)
        List<UUID> orgIds = resolveOrgIds(orgId, filter.isIncludeDescendants());

        // 3. Query thống kê tổng quan
        long totalUsers = ourRepository.countUsersByOrgIds(orgIds);
        long activeUsers = 0;
        long inactiveUsers = 0;

        if (filter.getActive() != null) {
            // Nếu lọc theo active/inactive cụ thể
            if (filter.getActive()) {
                activeUsers = ourRepository.countUsersByOrgIdsAndActive(orgIds, true);
            } else {
                inactiveUsers = ourRepository.countUsersByOrgIdsAndActive(orgIds, false);
            }
        } else {
            activeUsers = ourRepository.countUsersByOrgIdsAndActive(orgIds, true);
            inactiveUsers = ourRepository.countUsersByOrgIdsAndActive(orgIds, false);
        }

        // 4. Thống kê theo từng đơn vị
        List<OrgUserSummaryDto> orgSummaries = buildOrgSummaries(orgIds);

        // 5. Thống kê theo vai trò
        List<RoleUserSummaryDto> roleSummaries = buildRoleSummaries(orgIds);

        // 6. Build response
        UserReportResponseDto dto = new UserReportResponseDto();
        dto.setTotalUsers(totalUsers);
        dto.setActiveUsers(activeUsers);
        dto.setInactiveUsers(inactiveUsers);
        dto.setOrgSummaries(orgSummaries);
        dto.setRoleSummaries(roleSummaries);
        return dto;
    }

    /**
     * Giải quyết danh sách org IDs dựa trên tham số includeDescendants.
     * Nếu includeDescendants = true, lấy tất cả đơn vị con trong cây.
     */
    private List<UUID> resolveOrgIds(UUID orgId, boolean includeDescendants) {
        if (!includeDescendants) {
            return List.of(orgId);
        }

        OrganizationEntity org = organizationService.getEntityById(orgId);
        String pathPrefix = org.getPath() + "/%";
        return orgRepository.findDescendantOrgIds(orgId, pathPrefix);
    }

    /**
     * Xây dựng thống kê người dùng theo từng đơn vị trong danh sách.
     */
    private List<OrgUserSummaryDto> buildOrgSummaries(List<UUID> orgIds) {
        List<Object[]> orgStats = ourRepository.countUsersGroupByOrg(orgIds);
        if (orgStats.isEmpty()) {
            return List.of();
        }

        // Lấy thông tin tên của các org
        List<OrganizationEntity> orgs = orgRepository.findAllById(orgIds);
        Map<UUID, OrganizationEntity> orgMap = orgs.stream()
                .collect(Collectors.toMap(OrganizationEntity::getId, o -> o));

        List<OrgUserSummaryDto> summaries = new ArrayList<>();
        for (Object[] row : orgStats) {
            UUID statOrgId = (UUID) row[0];
            long total = ((Number) row[1]).longValue();
            long active = ((Number) row[2]).longValue();
            long inactive = ((Number) row[3]).longValue();

            OrgUserSummaryDto dto = new OrgUserSummaryDto();
            dto.setOrgId(statOrgId);
            dto.setOrgName(orgMap.containsKey(statOrgId) ? orgMap.get(statOrgId).getName() : "Unknown");
            dto.setOrgPath(orgMap.containsKey(statOrgId) ? orgMap.get(statOrgId).getPath() : "");
            dto.setTotalUsers(total);
            dto.setActiveUsers(active);
            dto.setInactiveUsers(inactive);
            summaries.add(dto);
        }
        return summaries;
    }

    /**
     * Xây dựng thống kê người dùng theo vai trò trong danh sách đơn vị.
     */
    private List<RoleUserSummaryDto> buildRoleSummaries(List<UUID> orgIds) {
        List<Object[]> roleStats = ourRepository.countUsersGroupByRole(orgIds);
        if (roleStats.isEmpty()) {
            return List.of();
        }

        List<RoleUserSummaryDto> summaries = new ArrayList<>();
        for (Object[] row : roleStats) {
            UUID roleId = (UUID) row[0];
            String roleName = (String) row[1];
            long total = ((Number) row[2]).longValue();

            RoleUserSummaryDto dto = new RoleUserSummaryDto();
            dto.setRoleId(roleId);
            dto.setRoleName(roleName);
            dto.setTotalUsers(total);
            summaries.add(dto);
        }
        return summaries;
    }
}