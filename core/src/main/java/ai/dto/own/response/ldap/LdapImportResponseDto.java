package ai.dto.own.response.ldap;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Kết quả import một loạt user LDAP.
 */
@Data
@Builder
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LdapImportResponseDto {
    int successCount;
    int failedCount;
    List<LdapImportItemResult> results;

    @Data
    @Builder
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class LdapImportItemResult {
        String ldapUserId;
        boolean success;
        String message;
        String importedUserId;
    }
}