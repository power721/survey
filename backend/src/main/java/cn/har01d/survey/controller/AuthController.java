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

import cn.har01d.survey.config.RateLimiter;
import cn.har01d.survey.dto.ApiResponse;
import cn.har01d.survey.dto.auth.AuthResponse;
import cn.har01d.survey.dto.auth.LoginRequest;
import cn.har01d.survey.dto.auth.RegisterRequest;
import cn.har01d.survey.dto.auth.UpdateProfileRequest;
import cn.har01d.survey.dto.auth.UserDto;
import cn.har01d.survey.exception.BusinessException;
import cn.har01d.survey.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RateLimiter rateLimiter;

    public AuthController(AuthService authService, RateLimiter rateLimiter) {
        this.authService = authService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request,
                                                               HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        if (!rateLimiter.isAllowed("register:" + ip, 5, 3600_000)) {
            throw new BusinessException("error.rate.limit", HttpStatus.TOO_MANY_REQUESTS);
        }
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.ok("Registration successful", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request,
                                                            HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        if (!rateLimiter.isAllowed("login:" + ip, 10, 60_000)) {
            throw new BusinessException("error.rate.limit", HttpStatus.TOO_MANY_REQUESTS);
        }
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
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
