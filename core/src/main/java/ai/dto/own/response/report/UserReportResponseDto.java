package ai.dto.own.response.report;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "User report response")
public class UserReportResponseDto {

    @Schema(description = "Total number of users", example = "100")
    long totalUsers;

    @Schema(description = "Number of active users", example = "80")
    long activeUsers;

    @Schema(description = "Number of inactive users", example = "20")
    long inactiveUsers;

    @Schema(description = "Summary breakdown by organization")
    List<OrgUserSummaryDto> orgSummaries;

    @Schema(description = "Summary breakdown by role")
    List<RoleUserSummaryDto> roleSummaries;

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Schema(description = "User summary by organization")
    public static class OrgUserSummaryDto {

        @Schema(description = "Organization ID")
        UUID orgId;

        @Schema(description = "Organization name", example = "Đơn vị A")
        String orgName;

        @Schema(description = "Organization path", example = "uuid-parent/uuid-child")
        String orgPath;

        @Schema(description = "Total users in this organization", example = "30")
        long totalUsers;

        @Schema(description = "Active users in this organization", example = "25")
        long activeUsers;

        @Schema(description = "Inactive users in this organization", example = "5")
        long inactiveUsers;
    }

    @Data
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @Schema(description = "User summary by role")
    public static class RoleUserSummaryDto {

        @Schema(description = "Role ID")
        UUID roleId;

        @Schema(description = "Role name", example = "Nhân viên")
        String roleName;

        @Schema(description = "Total users with this role", example = "40")
        long totalUsers;
    }
}