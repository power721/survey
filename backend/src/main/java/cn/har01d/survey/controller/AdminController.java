package cn.har01d.survey.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.har01d.survey.dto.ApiResponse;
import cn.har01d.survey.dto.DashboardStatsDto;
import cn.har01d.survey.service.DashboardService;
import cn.har01d.survey.service.SystemConfigService;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final SystemConfigService configService;
    private final DashboardService dashboardService;

    public AdminController(SystemConfigService configService, DashboardService dashboardService) {
        this.configService = configService;
        this.dashboardService = dashboardService;
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
}
