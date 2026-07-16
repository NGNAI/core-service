package ai.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import ai.entity.postgres.AuditLogEntity;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID>, JpaSpecificationExecutor<AuditLogEntity> {

    @Query("""
        SELECT COUNT(a)
        FROM AuditLogEntity a
        WHERE a.orgId IN :orgIds
        AND a.createdAt >= COALESCE(:from, a.createdAt)
        AND a.createdAt <= COALESCE(:to, a.createdAt)
        """)
    long countActionsByOrgIdsAndDateRange(
            @Param("orgIds") Collection<UUID> orgIds,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("""
        SELECT COUNT(DISTINCT a.userId)
        FROM AuditLogEntity a
        WHERE a.orgId IN :orgIds
        AND a.createdAt >= COALESCE(:from, a.createdAt)
        AND a.createdAt <= COALESCE(:to, a.createdAt)
        """)
    long countUniqueActiveUsersByOrgIdsAndDateRange(
            @Param("orgIds") Collection<UUID> orgIds,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("""
        SELECT a.resource as resource, COUNT(a) as cnt
        FROM AuditLogEntity a
        WHERE a.orgId IN :orgIds
        AND a.createdAt >= COALESCE(:from, a.createdAt)
        AND a.createdAt <= COALESCE(:to, a.createdAt)
        GROUP BY a.resource
        ORDER BY cnt DESC
        """)
    List<Object[]> countActionsGroupByResource(
            @Param("orgIds") Collection<UUID> orgIds,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("""
        SELECT a.action as action, COUNT(a) as cnt
        FROM AuditLogEntity a
        WHERE a.orgId IN :orgIds
        AND a.createdAt >= COALESCE(:from, a.createdAt)
        AND a.createdAt <= COALESCE(:to, a.createdAt)
        GROUP BY a.action
        ORDER BY cnt DESC
        """)
    List<Object[]> countActionsGroupByAction(
            @Param("orgIds") Collection<UUID> orgIds,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("""
        SELECT a.userId as userId, a.userName as userName, COUNT(a) as cnt
        FROM AuditLogEntity a
        WHERE a.orgId IN :orgIds
        AND a.createdAt >= COALESCE(:from, a.createdAt)
        AND a.createdAt <= COALESCE(:to, a.createdAt)
        GROUP BY a.userId, a.userName
        ORDER BY cnt DESC
        """)
    List<Object[]> findTopActiveUsers(
            @Param("orgIds") Collection<UUID> orgIds,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    @Query("""
        SELECT a.userId as userId, a.userName as userName, COUNT(a) as cnt, MAX(a.createdAt) as lastLogin
        FROM AuditLogEntity a
        WHERE a.orgId IN :orgIds
        AND a.action = 'LOGIN'
        AND a.createdAt >= COALESCE(:from, a.createdAt)
        AND a.createdAt <= COALESCE(:to, a.createdAt)
        GROUP BY a.userId, a.userName
        ORDER BY cnt DESC
        """)
    List<Object[]> findLoginFrequency(
            @Param("orgIds") Collection<UUID> orgIds,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("""
        SELECT CAST(a.createdAt AS date) as date,
               COUNT(a) as actionCount,
               SUM(CASE WHEN a.action = 'LOGIN' THEN 1 ELSE 0 END) as loginCount
        FROM AuditLogEntity a
        WHERE a.orgId IN :orgIds
        AND a.createdAt >= COALESCE(:from, a.createdAt)
        AND a.createdAt <= COALESCE(:to, a.createdAt)
        GROUP BY CAST(a.createdAt AS date)
        ORDER BY date ASC
        """)
    List<Object[]> findDailyActivityTrend(
            @Param("orgIds") Collection<UUID> orgIds,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("""
        SELECT COUNT(a)
        FROM AuditLogEntity a
        WHERE a.orgId IN :orgIds
        AND a.action = 'LOGIN'
        AND a.createdAt >= COALESCE(:from, a.createdAt)
        AND a.createdAt <= COALESCE(:to, a.createdAt)
        """)
    long countLoginsByOrgIdsAndDateRange(
            @Param("orgIds") Collection<UUID> orgIds,
            @Param("from") Instant from,
            @Param("to") Instant to);

    // Count actions by userId (user dashboard) - scoped by orgId
    @Query("""
        SELECT COUNT(a)
        FROM AuditLogEntity a
        WHERE a.userId = :userId
        AND a.orgId = :orgId
        AND a.createdAt >= COALESCE(:from, a.createdAt)
        AND a.createdAt <= COALESCE(:to, a.createdAt)
        """)
    long countActionsByUserIdAndDateRange(
            @Param("userId") UUID userId,
            @Param("orgId") UUID orgId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    // Count logins by userId (user dashboard) - scoped by orgId
    @Query("""
        SELECT COUNT(a)
        FROM AuditLogEntity a
        WHERE a.userId = :userId
        AND a.orgId = :orgId
        AND a.action = 'LOGIN'
        AND a.createdAt >= COALESCE(:from, a.createdAt)
        AND a.createdAt <= COALESCE(:to, a.createdAt)
        """)
    long countLoginsByUserIdAndDateRange(
            @Param("userId") UUID userId,
            @Param("orgId") UUID orgId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    // Daily activity trend by userId (user dashboard) - scoped by orgId
    @Query("""
        SELECT CAST(a.createdAt AS date) as date,
               COUNT(a) as actionCount,
               SUM(CASE WHEN a.action = 'LOGIN' THEN 1 ELSE 0 END) as loginCount
        FROM AuditLogEntity a
        WHERE a.userId = :userId
        AND a.orgId = :orgId
        AND a.createdAt >= COALESCE(:from, a.createdAt)
        AND a.createdAt <= COALESCE(:to, a.createdAt)
        GROUP BY CAST(a.createdAt AS date)
        ORDER BY date ASC
        """)
    List<Object[]> findDailyActivityTrendByUserId(
            @Param("userId") UUID userId,
            @Param("orgId") UUID orgId,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
