package cn.har01d.survey.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(length = 100)
    private String username;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(length = 50)
    private String entityType;

    private Long entityId;

    @Column(length = 1000)
    private String details;

    @Column(length = 100)
    private String ipAddress;

    @Column(nullable = false)
    private Instant createdAt;
}
