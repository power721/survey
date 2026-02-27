package cn.har01d.survey.controller;

import cn.har01d.survey.dto.ApiResponse;
import cn.har01d.survey.dto.auth.*;
import cn.har01d.survey.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.ok("Registration successful", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
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
}
