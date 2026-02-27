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

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username already exists", HttpStatus.CONFLICT);
        }
        String email = request.getEmail() != null && !request.getEmail().isBlank() ? request.getEmail().trim() : null;
        if (email != null && userRepository.existsByEmail(email)) {
            throw new BusinessException("Email already exists", HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(email)
                .nickname(request.getNickname() != null && !request.getNickname().isBlank() ? request.getNickname().trim() : generateNickname())
                .role(User.Role.USER)
                .enabled(true)
                .build();
        userRepository.save(user);

        String token = tokenProvider.generateToken(user.getUsername(), user.getRole().name());
        return new AuthResponse(token, user.getUsername(), user.getNickname(), user.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("User not found"));
        String token = tokenProvider.generateToken(user.getUsername(), user.getRole().name());
        return new AuthResponse(token, user.getUsername(), user.getNickname(), user.getRole().name());
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
            throw new BusinessException("Not authenticated", HttpStatus.UNAUTHORIZED);
        }
        return toDto(user);
    }

    public UserDto updateProfile(UpdateProfileRequest request) {
        User user = getCurrentUser();
        if (user == null) {
            throw new BusinessException("Not authenticated", HttpStatus.UNAUTHORIZED);
        }

        if (request.getNickname() != null) {
            user.setNickname(!request.getNickname().isBlank() ? request.getNickname().trim() : generateNickname());
        }

        String email = request.getEmail() != null && !request.getEmail().isBlank() ? request.getEmail().trim() : null;
        if (email != null && !email.equals(user.getEmail())) {
            if (userRepository.existsByEmail(email)) {
                throw new BusinessException("Email already exists", HttpStatus.CONFLICT);
            }
            user.setEmail(email);
        } else if (request.getEmail() != null && request.getEmail().isBlank()) {
            user.setEmail(null);
        }

        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar().isBlank() ? null : request.getAvatar().trim());
        }

        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            if (request.getOldPassword() == null || !passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
                throw new BusinessException("Old password is incorrect");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        userRepository.save(user);
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

    private UserDto toDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setEmail(user.getEmail());
        dto.setAvatar(user.getAvatar());
        dto.setRole(user.getRole().name());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}
