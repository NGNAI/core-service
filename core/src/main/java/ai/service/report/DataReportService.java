package ai.service.report;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import ai.dto.own.request.report.DataReportFilterDto;
import ai.dto.own.response.report.ContentStatsDto;
import ai.dto.own.response.report.DataIngestionDetailDto;
import ai.dto.own.response.report.DataIngestionDetailDto.OwnerIngestionSummary;
import ai.dto.own.response.report.DataReportResponseDto;
import ai.entity.postgres.OrganizationEntity;
import ai.enums.ApiResponseStatus;
import ai.exception.AppException;
import ai.repository.DataIngestionRepository;
import ai.repository.DraftRepository;
import ai.repository.NoteBookRepository;
import ai.repository.NoteRepository;
import ai.repository.OrganizationRepository;
import ai.repository.TopicRepository;
import ai.service.OrganizationService;
import ai.util.JwtUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DataReportService {

    DataIngestionRepository dataIngestionRepository;
    DraftRepository draftRepository;
    TopicRepository topicRepository;
    NoteBookRepository noteBookRepository;
    NoteRepository noteRepository;
    OrganizationRepository orgRepository;
    OrganizationService organizationService;

    /**
     * Lấy báo cáo tổng quan dữ liệu.
     */
    public DataReportResponseDto getDataReport(DataReportFilterDto filter) {
        UUID orgId = JwtUtil.getOrgId();
        if (orgId == null) {
            throw new AppException(ApiResponseStatus.ORG_ID_REQUIRED);
        }
        List<UUID> orgIds = resolveOrgIds(orgId, filter.isIncludeDescendants());
        Instant from = filter.getFrom();
        Instant to = filter.getTo();

        DataReportResponseDto dto = new DataReportResponseDto();

        // Tổng quan (filtered by from-to)
        dto.setTotalDataIngestions(dataIngestionRepository.countByDateRange(orgIds, from, to));
        dto.setTotalDrafts(draftRepository.countByDateRange(orgIds, from, to));
        dto.setTotalTopics(topicRepository.countByDateRange(orgIds, from, to));
        dto.setTotalNoteBooks(noteBookRepository.countByDateRange(orgIds, from, to));
        dto.setTotalNotes(noteRepository.countByDateRange(orgIds, from, to));

        // Chi tiết ingestion (filtered by from-to)
        dto.setIngestionDetail(buildIngestionDetail(orgIds, from, to, filter.getTopN()));

        // Thống kê nội dung (filtered by from-to)
        dto.setContentStats(buildContentStats(orgIds, from, to));

        return dto;
    }

    /**
     * Lấy chi tiết thống kê ingestion.
     */
    public DataIngestionDetailDto getIngestionDetail(DataReportFilterDto filter) {
        UUID orgId = JwtUtil.getOrgId();
        if (orgId == null) {
            throw new AppException(ApiResponseStatus.ORG_ID_REQUIRED);
        }
        List<UUID> orgIds = resolveOrgIds(orgId, filter.isIncludeDescendants());
        return buildIngestionDetail(orgIds, filter.getFrom(), filter.getTo(), filter.getTopN());
    }

    /**
     * Lấy top N người dùng nhập nhiều dữ liệu nhất.
     */
    public List<OwnerIngestionSummary> getTopOwners(DataReportFilterDto filter) {
        UUID orgId = JwtUtil.getOrgId();
        if (orgId == null) {
            throw new AppException(ApiResponseStatus.ORG_ID_REQUIRED);
        }
        List<UUID> orgIds = resolveOrgIds(orgId, filter.isIncludeDescendants());
        Instant from = filter.getFrom();
        Instant to = filter.getTo();

        return buildTopOwners(orgIds, from, to, filter.getTopN());
    }

    /**
     * Lấy thống kê nội dung.
     */
    public ContentStatsDto getContentStats(DataReportFilterDto filter) {
        UUID orgId = JwtUtil.getOrgId();
        if (orgId == null) {
            throw new AppException(ApiResponseStatus.ORG_ID_REQUIRED);
        }
        List<UUID> orgIds = resolveOrgIds(orgId, filter.isIncludeDescendants());
        return buildContentStats(orgIds, filter.getFrom(), filter.getTo());
    }

    private List<UUID> resolveOrgIds(UUID orgId, boolean includeDescendants) {
        if (!includeDescendants) {
            return List.of(orgId);
        }
        OrganizationEntity org = organizationService.getEntityById(orgId);
        String pathPrefix = org.getPath() + "/%";
        return orgRepository.findDescendantOrgIds(orgId, pathPrefix);
    }

    private DataIngestionDetailDto buildIngestionDetail(List<UUID> orgIds, Instant from, Instant to, int topN) {
        DataIngestionDetailDto dto = new DataIngestionDetailDto();

        // By status
        Map<String, Long> byStatus = new HashMap<>();
        dataIngestionRepository.countByStatusByOrgIdsAndDateRange(orgIds, from, to).forEach(row -> {
            Object status = row[0];
            byStatus.put(status != null ? status.toString() : "UNKNOWN", ((Number) row[1]).longValue());
        });
        dto.setByStatus(byStatus);

        // By source
        Map<String, Long> bySource = new HashMap<>();
        dataIngestionRepository.countBySourceByOrgIdsAndDateRange(orgIds, from, to).forEach(row -> {
            bySource.put(row[0] != null ? row[0].toString() : "UNKNOWN", ((Number) row[1]).longValue());
        });
        dto.setBySource(bySource);

        // By access level
        Map<String, Long> byAccessLevel = new HashMap<>();
        dataIngestionRepository.countByAccessLevelByOrgIdsAndDateRange(orgIds, from, to).forEach(row -> {
            byAccessLevel.put(row[0] != null ? row[0].toString() : "UNKNOWN", ((Number) row[1]).longValue());
        });
        dto.setByAccessLevel(byAccessLevel);

        // Top owners
        dto.setTopOwners(buildTopOwners(orgIds, from, to, topN));

        // Total file size
        dto.setTotalFileSize(dataIngestionRepository.sumFileSizeByOrgIdsAndDateRange(orgIds, from, to));

        // By content type
        Map<String, Long> byContentType = new HashMap<>();
        dataIngestionRepository.countByContentTypeAndDateRange(orgIds, from, to).forEach(row -> {
            byContentType.put(row[0] != null ? row[0].toString() : "UNKNOWN", ((Number) row[1]).longValue());
        });
        dto.setByContentType(byContentType);

        return dto;
    }

    private List<OwnerIngestionSummary> buildTopOwners(List<UUID> orgIds, Instant from, Instant to, int topN) {
        List<Object[]> rows = dataIngestionRepository.countByOwnerGroupByOwnerAndDateRange(orgIds, from, to, PageRequest.of(0, topN));
        List<OwnerIngestionSummary> result = new ArrayList<>();
        for (Object[] row : rows) {
            OwnerIngestionSummary s = new OwnerIngestionSummary();
            s.setUserId((UUID) row[0]);
            s.setUserName((String) row[1]);
            s.setIngestionCount(((Number) row[2]).longValue());
            s.setTotalFileSize(((Number) row[3]).longValue());
            result.add(s);
        }
        return result;
    }

    private ContentStatsDto buildContentStats(List<UUID> orgIds, Instant from, Instant to) {
        ContentStatsDto dto = new ContentStatsDto();

        // Drafts by type
        Map<String, Long> draftsByType = new HashMap<>();
        draftRepository.countByTypeByOrgIdsAndDateRange(orgIds, from, to).forEach(row -> {
            draftsByType.put((String) row[0], (Long) row[1]);
        });
        dto.setDraftsByType(draftsByType);

        // Topics, notebooks, notes counts
        dto.setTotalTopics(topicRepository.countByDateRange(orgIds, from, to));
        dto.setTotalNoteBooks(noteBookRepository.countByDateRange(orgIds, from, to));
        dto.setTotalNotes(noteRepository.countByDateRange(orgIds, from, to));

        // Notes by source type
        Map<String, Long> notesBySourceType = new HashMap<>();
        noteRepository.countBySourceTypeAndDateRange(orgIds, from, to).forEach(row -> {
            notesBySourceType.put(row[0] != null ? row[0].toString() : "UNKNOWN", ((Number) row[1]).longValue());
        });
        dto.setNotesBySourceType(notesBySourceType);

        return dto;
    }
}