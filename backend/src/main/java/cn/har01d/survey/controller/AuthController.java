package cn.har01d.survey.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.har01d.survey.service.RateLimitService;
import cn.har01d.survey.dto.ApiResponse;
import cn.har01d.survey.dto.auth.AuthResponse;
import cn.har01d.survey.dto.auth.LoginRequest;
import cn.har01d.survey.dto.auth.RegisterRequest;
import cn.har01d.survey.dto.auth.UpdateProfileRequest;
import cn.har01d.survey.dto.auth.UserDto;
import cn.har01d.survey.exception.BusinessException;
import cn.har01d.survey.service.AuditLogService;
import cn.har01d.survey.service.AuthService;
import cn.har01d.survey.service.SystemConfigService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RateLimitService rateLimitService;
    private final SystemConfigService configService;
    private final AuditLogService auditLogService;

    public AuthController(AuthService authService, RateLimitService rateLimitService,
                         SystemConfigService configService, AuditLogService auditLogService) {
        this.authService = authService;
        this.rateLimitService = rateLimitService;
        this.configService = configService;
        this.auditLogService = auditLogService;
    }

    private int getLoginMaxAttempts() {
        String val = configService.get(SystemConfigService.LOGIN_MAX_ATTEMPTS);
        if (!val.isEmpty()) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return 10;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request,
                                                              HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        if (!rateLimitService.isAllowed("register:" + ip, 5, 3600_000)) {
            auditLogService.log("REGISTER_FAILED", "User", null,
                    "Registration failed (rate limit): " + request.getUsername() + " from " + ip, null);
            throw new BusinessException("error.rate.limit", HttpStatus.TOO_MANY_REQUESTS);
        }
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.ok(ApiResponse.ok("Registration successful", response));
        } catch (Exception e) {
            auditLogService.log("REGISTER_FAILED", "User", null,
                    "Registration failed for user: " + request.getUsername() + " from " + ip + " - " + e.getMessage(), null);
            throw e;
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request,
                                                           HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        if (!rateLimitService.isAllowed("login:" + ip, getLoginMaxAttempts(), 60_000)) {
            auditLogService.log("LOGIN_FAILED", "User", null,
                    "Login failed (rate limit): " + request.getUsername() + " from " + ip, null);
            throw new BusinessException("error.rate.limit", HttpStatus.TOO_MANY_REQUESTS);
        }
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
        } catch (Exception e) {
            auditLogService.log("LOGIN_FAILED", "User", null,
                    "Login failed for user: " + request.getUsername() + " from " + ip + " - " + e.getMessage(), null);
            throw e;
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserDto>> getProfile() {
        UserDto profile = authService.getUserProfile();
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserDto>> updateProfile(@RequestBody UpdateProfileRequest request) {
        UserDto profile = authService.updateProfile(request);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated", profile));
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty()) {
            ip = ip.split(",")[0].trim();
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
