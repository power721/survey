package cn.har01d.survey.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.har01d.survey.dto.ApiResponse;
import cn.har01d.survey.service.SystemConfigService;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final SystemConfigService configService;

    public ConfigController(SystemConfigService configService) {
        this.configService = configService;
    }

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPublicConfig() {
        return ResponseEntity.ok(ApiResponse.ok(configService.getPublicConfig()));
    }
}
