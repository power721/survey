package cn.har01d.survey.dto;

import java.time.Instant;

import lombok.Data;

@Data
public class AuditLogDto {
    private Long id;
    private Long userId;
    private String username;
    private String action;
    private String entityType;
    private Long entityId;
    private String details;
    private String ipAddress;
    private Instant createdAt;
}
