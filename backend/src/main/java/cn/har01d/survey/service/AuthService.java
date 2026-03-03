package cn.har01d.survey.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import cn.har01d.survey.dto.auth.AuthResponse;
import cn.har01d.survey.dto.auth.LoginRequest;
import cn.har01d.survey.dto.auth.RegisterRequest;
import cn.har01d.survey.dto.auth.UpdateProfileRequest;
import cn.har01d.survey.dto.auth.UserDto;
import cn.har01d.survey.entity.User;
import cn.har01d.survey.exception.BusinessException;
import cn.har01d.survey.repository.UserRepository;
import cn.har01d.survey.security.JwtTokenProvider;
import cn.har01d.survey.util.GravatarUtil;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final SystemConfigService configService;
    private final AuditLogService auditLogService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtTokenProvider tokenProvider,
                       SystemConfigService configService, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.configService = configService;
        this.auditLogService = auditLogService;
    }

    public AuthResponse register(RegisterRequest request) {
        if ("false".equals(configService.get(SystemConfigService.REGISTER_ENABLED, "true"))) {
            throw new BusinessException("auth.register.disabled");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("auth.user.exists", HttpStatus.CONFLICT);
        }
        String email = request.getEmail() != null && !request.getEmail().isBlank() ? request.getEmail().trim() : null;
        if (email != null && userRepository.existsByEmail(email)) {
            throw new BusinessException("auth.email.exists", HttpStatus.CONFLICT);
        }

        User.Role role = userRepository.count() == 0 ? User.Role.ADMIN : User.Role.USER;
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(email)
                .nickname(request.getNickname() != null && !request.getNickname().isBlank() ? request.getNickname().trim() : generateNickname())
                .role(role)
                .enabled(true)
                .build();
        userRepository.save(user);

        auditLogService.log("USER_REGISTERED", "User", user.getId(), "User registered: " + user.getUsername(), user);

        String token = tokenProvider.generateToken(user.getUsername(), user.getRole().name());
        return new AuthResponse(token, user.getUsername(), user.getNickname(), resolveAvatar(user), user.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("auth.user.not.found"));
        String token = tokenProvider.generateToken(user.getUsername(), user.getRole().name());

        auditLogService.log("USER_LOGIN", "User", user.getId(), "User logged in: " + user.getUsername(), user);

        return new AuthResponse(token, user.getUsername(), user.getNickname(), resolveAvatar(user), user.getRole().name());
    }

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        String username = ((org.springframework.security.core.userdetails.User) auth.getPrincipal()).getUsername();
        return userRepository.findByUsername(username).orElse(null);
    }

    public UserDto getUserProfile() {
        User user = getCurrentUser();
        if (user == null) {
            throw new BusinessException("auth.not.authenticated", HttpStatus.UNAUTHORIZED);
        }
        return toDto(user);
    }

    public UserDto updateProfile(UpdateProfileRequest request) {
        User user = getCurrentUser();
        if (user == null) {
            throw new BusinessException("auth.not.authenticated", HttpStatus.UNAUTHORIZED);
        }

        StringBuilder changes = new StringBuilder();

        if (request.getNickname() != null) {
            String oldNickname = user.getNickname();
            String newNickname = !request.getNickname().isBlank() ? request.getNickname().trim() : generateNickname();
            user.setNickname(newNickname);
            if (!newNickname.equals(oldNickname)) {
                if (changes.length() > 0) changes.append(", ");
                changes.append("nickname: '").append(oldNickname).append("' -> '").append(newNickname).append("'");
            }
        }

        String email = request.getEmail() != null && !request.getEmail().isBlank() ? request.getEmail().trim() : null;
        if (email != null && !email.equals(user.getEmail())) {
            if (userRepository.existsByEmail(email)) {
                throw new BusinessException("auth.email.exists", HttpStatus.CONFLICT);
            }
            String oldEmail = user.getEmail();
            user.setEmail(email);
            if (changes.length() > 0) changes.append(", ");
            changes.append("email: '").append(oldEmail != null ? oldEmail : "").append("' -> '").append(email).append("'");
        } else if (request.getEmail() != null && request.getEmail().isBlank()) {
            String oldEmail = user.getEmail();
            user.setEmail(null);
            if (changes.length() > 0) changes.append(", ");
            changes.append("email: '").append(oldEmail != null ? oldEmail : "").append("' -> (empty)");
        }

        if (request.getAvatar() != null) {
            String oldAvatar = user.getAvatar();
            String newAvatar = request.getAvatar().isBlank() ? null : request.getAvatar().trim();
            user.setAvatar(newAvatar);
            if ((oldAvatar == null && newAvatar != null) || (oldAvatar != null && !oldAvatar.equals(newAvatar))) {
                if (changes.length() > 0) changes.append(", ");
                changes.append("avatar updated");
            }
        }

        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            if (request.getOldPassword() == null || !passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
                throw new BusinessException("auth.old.password.incorrect");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            if (changes.length() > 0) changes.append(", ");
            changes.append("password changed");
        }

        userRepository.save(user);

        if (changes.length() > 0) {
            auditLogService.log("USER_PROFILE_UPDATED", "User", user.getId(),
                    "User " + user.getUsername() + " updated profile: " + changes.toString(), user);
        }

        return toDto(user);
    }

    private String generateNickname() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder("用户");
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String resolveAvatar(User user) {
        if (user.getAvatar() != null && !user.getAvatar().isBlank()) {
            return user.getAvatar();
        }
        return GravatarUtil.getAvatarUrl(user.getEmail());
    }

    private UserDto toDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setEmail(user.getEmail());
        dto.setAvatar(resolveAvatar(user));
        dto.setRole(user.getRole().name());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}
