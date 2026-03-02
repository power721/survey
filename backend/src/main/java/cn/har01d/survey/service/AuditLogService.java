package cn.har01d.survey.service;

import java.time.Instant;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import cn.har01d.survey.entity.AuditLog;
import cn.har01d.survey.entity.User;
import cn.har01d.survey.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;
    private final HttpServletRequest request;

    public void log(String action, String entityType, Long entityId, String details, User user) {
        String ipAddress = getClientIp();
        AuditLog log = AuditLog.builder()
                .userId(user != null ? user.getId() : null)
                .username(user != null ? user.getUsername() : "anonymous")
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .ipAddress(ipAddress)
                .createdAt(Instant.now())
                .build();
        auditLogRepository.save(log);
    }

    public void log(String action, String entityType, Long entityId, String details) {
        log(action, entityType, entityId, details, null);
    }

    public Page<AuditLog> getAuditLogs(String action, String entityType, String username, Pageable pageable) {
        return auditLogRepository.findByFilters(action, entityType, username, pageable);
    }

    private String getClientIp() {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
