package cn.har01d.survey.service;

import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import cn.har01d.survey.config.OAuth2Properties;
import cn.har01d.survey.dto.auth.AuthResponse;
import cn.har01d.survey.entity.SocialAccount;
import cn.har01d.survey.entity.User;
import cn.har01d.survey.exception.BusinessException;
import cn.har01d.survey.repository.SocialAccountRepository;
import cn.har01d.survey.repository.UserRepository;
import cn.har01d.survey.security.JwtTokenProvider;

@Service
public class OAuth2Service {

    private static final Logger log = LoggerFactory.getLogger(OAuth2Service.class);

    private final OAuth2Properties properties;
    private final SystemConfigService configService;
    private final SocialAccountRepository socialAccountRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final RestTemplate restTemplate = new RestTemplate();

    public OAuth2Service(OAuth2Properties properties,
                         SystemConfigService configService,
                         SocialAccountRepository socialAccountRepository,
                         UserRepository userRepository,
                         PasswordEncoder passwordEncoder,
                         JwtTokenProvider tokenProvider) {
        this.properties = properties;
        this.configService = configService;
        this.socialAccountRepository = socialAccountRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    private void checkOAuth2Enabled() {
        if (!"true".equals(configService.get(SystemConfigService.OAUTH2_ENABLED, "false"))) {
            throw new BusinessException("auth.oauth2.disabled");
        }
    }

    private String getGithubClientId() {
        String val = configService.get(SystemConfigService.GITHUB_CLIENT_ID);
        return val.isEmpty() ? properties.getGithub().getClientId() : val;
    }

    private String getGithubClientSecret() {
        String val = configService.get(SystemConfigService.GITHUB_CLIENT_SECRET);
        return val.isEmpty() ? properties.getGithub().getClientSecret() : val;
    }

    private String getGoogleClientId() {
        String val = configService.get(SystemConfigService.GOOGLE_CLIENT_ID);
        return val.isEmpty() ? properties.getGoogle().getClientId() : val;
    }

    private String getGoogleClientSecret() {
        String val = configService.get(SystemConfigService.GOOGLE_CLIENT_SECRET);
        return val.isEmpty() ? properties.getGoogle().getClientSecret() : val;
    }

    public String getAuthorizationUrl(String provider) {
        checkOAuth2Enabled();
        return switch (provider.toLowerCase()) {
            case "github" -> UriComponentsBuilder.fromUriString("https://github.com/login/oauth/authorize")
                    .queryParam("client_id", getGithubClientId())
                    .queryParam("scope", "read:user user:email")
                    .queryParam("redirect_uri", properties.getFrontendUrl() + "/oauth2/callback/" + provider)
                    .build().toUriString();
            case "google" -> UriComponentsBuilder.fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                    .queryParam("client_id", getGoogleClientId())
                    .queryParam("response_type", "code")
                    .queryParam("scope", "openid email profile")
                    .queryParam("redirect_uri", properties.getFrontendUrl() + "/oauth2/callback/" + provider)
                    .build().toUriString();
            default -> throw new BusinessException("auth.oauth2.unsupported.provider");
        };
    }

    public AuthResponse handleCallback(String provider, String code) {
        checkOAuth2Enabled();
        return switch (provider.toLowerCase()) {
            case "github" -> handleGitHubCallback(code);
            case "google" -> handleGoogleCallback(code);
            default -> throw new BusinessException("auth.oauth2.unsupported.provider");
        };
    }

    private AuthResponse handleGitHubCallback(String code) {
        // Exchange code for access token
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        Map<String, String> body = Map.of(
                "client_id", getGithubClientId(),
                "client_secret", getGithubClientSecret(),
                "code", code,
                "redirect_uri", properties.getFrontendUrl() + "/oauth2/callback/github"
        );
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        @SuppressWarnings("unchecked")
        Map<String, Object> tokenResponse = restTemplate.postForObject(
                "https://github.com/login/oauth/access_token", request, Map.class);
        if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
            log.error("GitHub token exchange failed: {}", tokenResponse);
            throw new BusinessException("auth.oauth2.failed");
        }
        String accessToken = (String) tokenResponse.get("access_token");

        // Fetch user info
        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(accessToken);
        HttpEntity<Void> userRequest = new HttpEntity<>(userHeaders);
        @SuppressWarnings("unchecked")
        ResponseEntity<Map> userResponse = restTemplate.exchange(
                "https://api.github.com/user", HttpMethod.GET, userRequest, Map.class);
        Map<String, Object> userInfo = userResponse.getBody();
        if (userInfo == null) {
            throw new BusinessException("auth.oauth2.failed");
        }

        String providerId = String.valueOf(userInfo.get("id"));
        String name = (String) userInfo.get("name");
        String login = (String) userInfo.get("login");
        String email = (String) userInfo.get("email");
        String avatar = (String) userInfo.get("avatar_url");

        return findOrCreateUser(SocialAccount.Provider.GITHUB, providerId,
                name != null ? name : login, email, avatar);
    }

    private AuthResponse handleGoogleCallback(String code) {
        // Exchange code for tokens
        Map<String, String> body = Map.of(
                "code", code,
                "client_id", getGoogleClientId(),
                "client_secret", getGoogleClientSecret(),
                "redirect_uri", properties.getFrontendUrl() + "/oauth2/callback/google",
                "grant_type", "authorization_code"
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> tokenResponse = restTemplate.postForObject(
                "https://oauth2.googleapis.com/token", body, Map.class);
        if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
            log.error("Google token exchange failed: {}", tokenResponse);
            throw new BusinessException("auth.oauth2.failed");
        }
        String accessToken = (String) tokenResponse.get("access_token");

        // Fetch user info
        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(accessToken);
        HttpEntity<Void> userRequest = new HttpEntity<>(userHeaders);
        @SuppressWarnings("unchecked")
        ResponseEntity<Map> userResponse = restTemplate.exchange(
                "https://www.googleapis.com/oauth2/v2/userinfo", HttpMethod.GET, userRequest, Map.class);
        Map<String, Object> userInfo = userResponse.getBody();
        if (userInfo == null) {
            throw new BusinessException("auth.oauth2.failed");
        }

        String providerId = String.valueOf(userInfo.get("id"));
        String name = (String) userInfo.get("name");
        String email = (String) userInfo.get("email");
        String avatar = (String) userInfo.get("picture");

        return findOrCreateUser(SocialAccount.Provider.GOOGLE, providerId, name, email, avatar);
    }

    private AuthResponse findOrCreateUser(SocialAccount.Provider provider, String providerId,
                                          String name, String email, String avatar) {
        SocialAccount socialAccount = socialAccountRepository
                .findByProviderAndProviderId(provider, providerId)
                .orElse(null);

        User user;
        if (socialAccount != null) {
            user = socialAccount.getUser();
            // Update social account info
            socialAccount.setName(name);
            socialAccount.setEmail(email);
            socialAccount.setAvatar(avatar);
            socialAccountRepository.save(socialAccount);
        } else {
            // Try to link by email
            if (email != null && !email.isBlank()) {
                user = userRepository.findByEmail(email).orElse(null);
            } else {
                user = null;
            }

            if (user == null) {
                // Create new user
                String username = generateUniqueUsername(provider, name);
                user = User.builder()
                        .username(username)
                        .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                        .nickname(name != null ? name : username)
                        .email(email)
                        .avatar(avatar)
                        .role(User.Role.USER)
                        .enabled(true)
                        .build();
                userRepository.save(user);
            }

            socialAccount = SocialAccount.builder()
                    .provider(provider)
                    .providerId(providerId)
                    .name(name)
                    .email(email)
                    .avatar(avatar)
                    .user(user)
                    .build();
            socialAccountRepository.save(socialAccount);
        }

        String token = tokenProvider.generateToken(user.getUsername(), user.getRole().name());
        return new AuthResponse(token, user.getUsername(), user.getNickname(), user.getRole().name());
    }

    private String generateUniqueUsername(SocialAccount.Provider provider, String name) {
        String base = provider.name().toLowerCase() + "_" + (name != null ? name.replaceAll("[^a-zA-Z0-9]", "") : "user");
        if (base.length() > 40) {
            base = base.substring(0, 40);
        }
        String username = base;
        int suffix = 1;
        while (userRepository.existsByUsername(username)) {
            username = base + suffix++;
        }
        return username;
    }
}
