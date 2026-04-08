package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.dto.auth.AuthUserResponse;
import ba.unsa.etf.NBP.dto.auth.CreateUserRequest;
import ba.unsa.etf.NBP.dto.auth.CreatedUserResponse;
import ba.unsa.etf.NBP.dto.auth.LoginRequest;
import ba.unsa.etf.NBP.model.Role;
import ba.unsa.etf.NBP.model.User;
import ba.unsa.etf.NBP.model.UserSession;
import ba.unsa.etf.NBP.repository.UserRepository;
import ba.unsa.etf.NBP.repository.UserSessionRepository;
import ba.unsa.etf.NBP.security.JwtTokenService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, userSessionRepository, passwordEncoder, jwtTokenService);
    }

    @Test
    void loginReturnsUnauthorizedWhenUsernameDoesNotExist() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setUsername("missing");
        request.setPassword("secret");

        Optional<AuthUserResponse> result = authService.login(request);

        assertFalse(result.isPresent());
        verify(passwordEncoder, never()).matches(any(), any());
        verify(userSessionRepository, never()).save(any());
    }

    @Test
    void loginReturnsUnauthorizedWhenPasswordIsInvalid() {
        User user = buildUser();
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", user.getPassword())).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setUsername("john");
        request.setPassword("wrong");

        Optional<AuthUserResponse> result = authService.login(request);

        assertFalse(result.isPresent());
        verify(userSessionRepository, never()).save(any());
        verify(userSessionRepository, never()).deleteById(any());
    }

    @Test
    void loginDeletesOldSessionsAndCreatesSingleNewSession() {
        User user = buildUser();
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", user.getPassword())).thenReturn(true);
        when(jwtTokenService.createAccessToken(eq(user), anyString())).thenReturn("access-token");
        when(jwtTokenService.createRefreshToken(eq(user), anyString())).thenReturn("refresh-token");

        LoginRequest request = new LoginRequest();
        request.setUsername("john");
        request.setPassword("secret");

        Optional<AuthUserResponse> result = authService.login(request);

        assertTrue(result.isPresent());
        AuthUserResponse response = result.get();
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertNotNull(response.getSessionId());
        assertEquals(user.getId(), response.getUserId());
        assertEquals("Student", response.getRoleName());

        verify(userSessionRepository, times(1)).deleteById(user.getId());
        ArgumentCaptor<UserSession> sessionCaptor = ArgumentCaptor.forClass(UserSession.class);
        verify(userSessionRepository, times(1)).save(sessionCaptor.capture());

        UserSession savedSession = sessionCaptor.getValue();
        assertEquals(user.getId(), savedSession.getUserId());
        assertNotNull(savedSession.getCreatedAt());
        assertNotNull(savedSession.getExpiresAt());
        assertTrue(savedSession.getExpiresAt().isAfter(savedSession.getCreatedAt()));
    }

    @Test
    void getCurrentUserBySessionReturnsEmptyWhenSessionExpiredOrMissing() {
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(jwtTokenService.parseClaims("access-token")).thenReturn(Optional.of(claims));
        when(jwtTokenService.isRefreshToken(claims)).thenReturn(false);
        when(jwtTokenService.extractSessionId(claims)).thenReturn("fake");
        when(userSessionRepository.findActiveBySessionId(eq("fake"), any(LocalDateTime.class))).thenReturn(Optional.empty());

        Optional<AuthUserResponse> result = authService.getCurrentUserByToken("Bearer access-token");

        assertFalse(result.isPresent());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void getCurrentUserBySessionReturnsMappedUserWhenSessionIsValid() {
        User user = buildUser();
        UserSession session = new UserSession("session-1", user.getId(), LocalDateTime.now(), LocalDateTime.now().plusMinutes(30));
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(jwtTokenService.parseClaims("access-token")).thenReturn(Optional.of(claims));
        when(jwtTokenService.isRefreshToken(claims)).thenReturn(false);
        when(jwtTokenService.extractSessionId(claims)).thenReturn("session-1");
        when(jwtTokenService.createAccessToken(eq(user), eq("session-1"))).thenReturn("new-access");

        when(userSessionRepository.findActiveBySessionId(eq("session-1"), any(LocalDateTime.class))).thenReturn(Optional.of(session));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        Optional<AuthUserResponse> result = authService.getCurrentUserByToken("Bearer access-token");

        assertTrue(result.isPresent());
        AuthUserResponse response = result.get();
        assertEquals("new-access", response.getAccessToken());
        assertEquals("session-1", response.getSessionId());
        assertNull(response.getRefreshToken());
        assertEquals(user.getEmail(), response.getEmail());
        assertEquals(user.getRole().getRoleName(), response.getRoleName());
    }

    @Test
    void authenticateSessionReturnsUserForValidSession() {
        User user = buildUser();
        UserSession session = new UserSession("session-2", user.getId(), LocalDateTime.now(), LocalDateTime.now().plusMinutes(30));
        Claims claims = org.mockito.Mockito.mock(Claims.class);

        when(jwtTokenService.parseClaims("access-token")).thenReturn(Optional.of(claims));
        when(jwtTokenService.isRefreshToken(claims)).thenReturn(false);
        when(jwtTokenService.extractSessionId(claims)).thenReturn("session-2");

        when(userSessionRepository.findActiveBySessionId(eq("session-2"), any(LocalDateTime.class))).thenReturn(Optional.of(session));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        Optional<User> result = authService.authenticateSession("Bearer access-token");

        assertTrue(result.isPresent());
        assertSame(user, result.get());
    }

    @Test
    void logoutDeletesSessionBySessionId() {
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(jwtTokenService.parseClaims("access-token")).thenReturn(Optional.of(claims));
        when(jwtTokenService.isRefreshToken(claims)).thenReturn(false);
        when(jwtTokenService.extractSessionId(claims)).thenReturn("session-3");

        authService.logout("Bearer access-token");
        verify(userSessionRepository, times(1)).deleteBySessionId("session-3");
    }

    @Test
    void refreshRotatesSessionAndReturnsNewTokens() {
        User user = buildUser();
        UserSession session = new UserSession("old-session", user.getId(), LocalDateTime.now().minusHours(1), LocalDateTime.now().minusMinutes(1));
        Claims claims = org.mockito.Mockito.mock(Claims.class);

        when(jwtTokenService.parseClaims("refresh-token")).thenReturn(Optional.of(claims));
        when(jwtTokenService.isRefreshToken(claims)).thenReturn(true);
        when(jwtTokenService.extractUserId(claims)).thenReturn(user.getId());
        when(jwtTokenService.extractSessionId(claims)).thenReturn("old-session");
        when(userSessionRepository.findById("old-session")).thenReturn(Optional.of(session));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(jwtTokenService.createAccessToken(eq(user), anyString())).thenReturn("new-access-token");
        when(jwtTokenService.createRefreshToken(eq(user), anyString())).thenReturn("new-refresh-token");

        Optional<AuthUserResponse> result = authService.refresh("refresh-token");

        assertTrue(result.isPresent());
        assertEquals("new-access-token", result.get().getAccessToken());
        assertEquals("new-refresh-token", result.get().getRefreshToken());
        verify(userSessionRepository).deleteBySessionId("old-session");
        verify(userSessionRepository).save(any(UserSession.class));
    }

    @Test
    void refreshFailsWhenOriginalSessionWasDeleted() {
        User user = buildUser();
        Claims claims = org.mockito.Mockito.mock(Claims.class);

        when(jwtTokenService.parseClaims("refresh-token")).thenReturn(Optional.of(claims));
        when(jwtTokenService.isRefreshToken(claims)).thenReturn(true);
        when(jwtTokenService.extractUserId(claims)).thenReturn(user.getId());
        when(jwtTokenService.extractSessionId(claims)).thenReturn("deleted-session");
        when(userSessionRepository.findById("deleted-session")).thenReturn(Optional.empty());

        Optional<AuthUserResponse> result = authService.refresh("refresh-token");

        assertFalse(result.isPresent());
        verify(userSessionRepository, never()).save(any(UserSession.class));
    }

    @Test
    void createUserByAdminCreatesHashedPasswordAndNullAddress() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("newstudent");
        request.setPassword("raw-pass");
        request.setFirstName("New");
        request.setLastName("Student");
        request.setEmail("new@student.ba");
        request.setRoleId(1L);
        request.setBirthDate(LocalDate.of(2001, 5, 10));

        Role role = new Role(1L, "student");
        when(userRepository.existsByUsername("newstudent")).thenReturn(false);
        when(userRepository.findRoleById(1L)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("raw-pass")).thenReturn("bcrypt-hash");
        when(userRepository.createUser(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(101L);
            return Optional.of(user);
        });

        Optional<CreatedUserResponse> result = authService.createUserByAdmin(request);

        assertTrue(result.isPresent());
        CreatedUserResponse created = result.get();
        assertEquals(101L, created.getUserId());
        assertEquals("student", created.getRoleName());
        assertEquals(request.getBirthDate(), created.getBirthDate());
        assertNull(created.getAddressId());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).createUser(userCaptor.capture());
        User persisted = userCaptor.getValue();
        assertEquals("bcrypt-hash", persisted.getPassword());
        assertNull(persisted.getAddressId());
    }

    @Test
    void createUserByAdminRejectsUnsupportedRole() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("newuser");
        request.setPassword("raw-pass");
        request.setFirstName("New");
        request.setLastName("User");
        request.setEmail("new@user.ba");
        request.setRoleId(9L);
        request.setBirthDate(LocalDate.of(2000, 1, 1));

        Optional<CreatedUserResponse> result = authService.createUserByAdmin(request);

        assertFalse(result.isPresent());
        verify(userRepository, never()).createUser(any(User.class));
    }

    private User buildUser() {
        Role role = new Role(1L, "Student");
        return new User(11L, "john", "$2a$10$hash", "John", "Doe", "john@etf.unsa.ba", LocalDate.of(2000, 1, 1), null, role);
    }
}




