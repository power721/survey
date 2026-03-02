package cn.har01d.survey.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cn.har01d.survey.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    @Query("SELECT a FROM AuditLog a WHERE " +
            "(:action IS NULL OR a.action = :action) AND " +
            "(:entityType IS NULL OR a.entityType = :entityType) AND " +
            "(:username IS NULL OR a.username LIKE %:username%) " +
            "ORDER BY a.createdAt DESC")
    Page<AuditLog> findByFilters(
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("username") String username,
            Pageable pageable
    );
}
