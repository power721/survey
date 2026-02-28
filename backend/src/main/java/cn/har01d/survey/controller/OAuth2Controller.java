package cn.har01d.survey.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.har01d.survey.dto.ApiResponse;
import cn.har01d.survey.dto.auth.AuthResponse;
import cn.har01d.survey.service.OAuth2Service;

@RestController
@RequestMapping("/api/auth/oauth2")
public class OAuth2Controller {

    private final OAuth2Service oauth2Service;

    public OAuth2Controller(OAuth2Service oauth2Service) {
        this.oauth2Service = oauth2Service;
    }

    @GetMapping("/{provider}")
    public ResponseEntity<ApiResponse<String>> getAuthorizationUrl(@PathVariable String provider) {
        String url = oauth2Service.getAuthorizationUrl(provider);
        return ResponseEntity.ok(ApiResponse.ok(url));
    }

    @PostMapping("/{provider}/callback")
    public ResponseEntity<ApiResponse<AuthResponse>> handleCallback(
            @PathVariable String provider,
            @RequestBody CallbackRequest request) {
        AuthResponse response = oauth2Service.handleCallback(provider, request.code());
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    public record CallbackRequest(String code) {}
}
