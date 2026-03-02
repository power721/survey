package cn.har01d.survey.controller;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cn.har01d.survey.dto.ApiResponse;
import cn.har01d.survey.dto.DashboardStatsDto;
import cn.har01d.survey.entity.AuditLog;
import cn.har01d.survey.service.AuditLogService;
import cn.har01d.survey.service.DashboardService;
import cn.har01d.survey.service.SystemConfigService;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final SystemConfigService configService;
    private final DashboardService dashboardService;
    private final AuditLogService auditLogService;

    public AdminController(SystemConfigService configService, DashboardService dashboardService, AuditLogService auditLogService) {
        this.configService = configService;
        this.dashboardService = dashboardService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<Map<String, String>>> getConfig() {
        return ResponseEntity.ok(ApiResponse.ok(configService.getAll()));
    }

    @PutMapping("/config")
    public ResponseEntity<ApiResponse<Void>> updateConfig(@RequestBody Map<String, String> configs) {
        configService.updateAll(configs);
        return ResponseEntity.ok(ApiResponse.ok("Config updated", null));
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<ApiResponse<DashboardStatsDto>> getDashboardStats() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getStats()));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String username,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AuditLog> logs = auditLogService.getAuditLogs(action, entityType, username, pageable);
        return ResponseEntity.ok(ApiResponse.ok(logs));
    }
}
