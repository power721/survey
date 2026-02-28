package cn.har01d.survey.service;

import cn.har01d.survey.dto.auth.*;
import cn.har01d.survey.entity.User;
import cn.har01d.survey.exception.BusinessException;
import cn.har01d.survey.repository.UserRepository;
import cn.har01d.survey.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .password("encoded_password")
                .nickname("TestNick")
                .email("test@example.com")
                .role(User.Role.USER)
                .enabled(true)
                .build();
    }

    // --- register ---

    @Test
    void register_success() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("password123");
        request.setNickname("Nick");
        request.setEmail("new@example.com");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenProvider.generateToken(eq("newuser"), eq("ADMIN"))).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("newuser", response.getUsername());
        assertEquals("Nick", response.getNickname());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_usernameAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existing");
        request.setPassword("password123");

        when(userRepository.existsByUsername("existing")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.register(request));
        assertEquals("auth.user.exists", ex.getMessage());
    }

    @Test
    void register_emailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("password123");
        request.setEmail("taken@example.com");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.register(request));
        assertEquals("auth.email.exists", ex.getMessage());
    }

    @Test
    void register_emptyEmailShouldNotCheckDuplicate() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("password123");
        request.setEmail("");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenProvider.generateToken(eq("newuser"), eq("ADMIN"))).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        verify(userRepository, never()).existsByEmail(anyString());
    }

    @Test
    void register_noNickname_generatesRandomNickname() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("password123");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenProvider.generateToken(eq("newuser"), eq("ADMIN"))).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertTrue(response.getNickname().startsWith("用户"));
        assertEquals(10, response.getNickname().length()); // "用户" (2 chars) + 8 chars
    }

    // --- login ---

    @Test
    void login_success() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(tokenProvider.generateToken("testuser", "USER")).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("testuser", response.getUsername());
        assertEquals("TestNick", response.getNickname());
    }

    @Test
    void login_userNotFound() {
        LoginRequest request = new LoginRequest();
        request.setUsername("unknown");
        request.setPassword("password123");

        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> authService.login(request));
    }

    // --- getCurrentUser ---

    @Test
    void getCurrentUser_noAuthentication() {
        SecurityContextHolder.clearContext();
        assertNull(authService.getCurrentUser());
    }

    @Test
    void getCurrentUser_anonymousUser() {
        Authentication auth = new UsernamePasswordAuthenticationToken("anonymousUser", null);
        SecurityContextHolder.getContext().setAuthentication(auth);
        assertNull(authService.getCurrentUser());
    }

    @Test
    void getCurrentUser_authenticated() {
        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User("testuser", "", Collections.emptyList());
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        User user = authService.getCurrentUser();
        assertNotNull(user);
        assertEquals("testuser", user.getUsername());
    }

    // --- getUserProfile ---

    @Test
    void getUserProfile_notAuthenticated() {
        SecurityContextHolder.clearContext();
        assertThrows(BusinessException.class, () -> authService.getUserProfile());
    }

    @Test
    void getUserProfile_success() {
        setAuthenticatedUser();

        UserDto dto = authService.getUserProfile();

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("testuser", dto.getUsername());
        assertEquals("TestNick", dto.getNickname());
        assertEquals("test@example.com", dto.getEmail());
    }

    // --- updateProfile ---

    @Test
    void updateProfile_notAuthenticated() {
        SecurityContextHolder.clearContext();
        UpdateProfileRequest request = new UpdateProfileRequest();
        assertThrows(BusinessException.class, () -> authService.updateProfile(request));
    }

    @Test
    void updateProfile_updateNickname() {
        setAuthenticatedUser();
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNickname("NewNick");

        UserDto dto = authService.updateProfile(request);

        assertEquals("NewNick", dto.getNickname());
    }

    @Test
    void updateProfile_blankNickname_generatesRandom() {
        setAuthenticatedUser();
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNickname("  ");

        UserDto dto = authService.updateProfile(request);

        assertTrue(dto.getNickname().startsWith("用户"));
        assertEquals(10, dto.getNickname().length());
    }

    @Test
    void updateProfile_updateEmail() {
        setAuthenticatedUser();
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setEmail("new@example.com");

        UserDto dto = authService.updateProfile(request);

        assertEquals("new@example.com", dto.getEmail());
    }

    @Test
    void updateProfile_emailAlreadyExists() {
        setAuthenticatedUser();
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setEmail("taken@example.com");

        assertThrows(BusinessException.class, () -> authService.updateProfile(request));
    }

    @Test
    void updateProfile_blankEmailClearsEmail() {
        setAuthenticatedUser();
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setEmail("");

        UserDto dto = authService.updateProfile(request);

        assertNull(dto.getEmail());
    }

    @Test
    void updateProfile_changePassword_success() {
        setAuthenticatedUser();
        when(passwordEncoder.matches("oldpass", "encoded_password")).thenReturn(true);
        when(passwordEncoder.encode("newpass")).thenReturn("new_encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setOldPassword("oldpass");
        request.setNewPassword("newpass");

        UserDto dto = authService.updateProfile(request);

        assertNotNull(dto);
        verify(passwordEncoder).encode("newpass");
    }

    @Test
    void updateProfile_changePassword_wrongOldPassword() {
        setAuthenticatedUser();
        when(passwordEncoder.matches("wrongold", "encoded_password")).thenReturn(false);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setOldPassword("wrongold");
        request.setNewPassword("newpass");

        assertThrows(BusinessException.class, () -> authService.updateProfile(request));
    }

    // --- helper ---

    private void setAuthenticatedUser() {
        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User("testuser", "", Collections.emptyList());
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    }
}
