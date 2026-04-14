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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    private static final Long STUDENT_ROLE_ID = 1L;
    private static final Long PROFESSOR_ROLE_ID = 2L;
    private static final Long ADMIN_ROLE_ID = 3L;

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(UserRepository userRepository,
                       UserSessionRepository userSessionRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    public Optional<AuthUserResponse> login(LoginRequest request) {
        Optional<User> foundUser = userRepository.findByUsername(request.getUsername());
        if (foundUser.isEmpty()) {
            return Optional.empty();
        }

        User user = foundUser.get();
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return Optional.empty();
        }

        userSessionRepository.deleteById(user.getId());

        String newSessionId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        UserSession userSession = new UserSession();
        userSession.setSessionId(newSessionId);
        userSession.setUserId(user.getId());
        userSession.setCreatedAt(now);
        userSession.setExpiresAt(now.plusMinutes(30));
        userSessionRepository.save(userSession);

        return Optional.of(toAuthUserResponse(user, newSessionId, true));
    }

    public Optional<AuthUserResponse> refresh(String refreshToken) {
        Optional<String> rawToken = extractToken(refreshToken);
        if (rawToken.isEmpty()) {
            return Optional.empty();
        }

        Optional<Claims> claimsOptional = jwtTokenService.parseClaims(rawToken.get());
        if (claimsOptional.isEmpty()) {
            return Optional.empty();
        }

        Claims claims = claimsOptional.get();
        if (!jwtTokenService.isRefreshToken(claims)) {
            return Optional.empty();
        }

        Long userId = jwtTokenService.extractUserId(claims);
        String oldSessionId = jwtTokenService.extractSessionId(claims);
        if (userId == null || oldSessionId == null || oldSessionId.isBlank()) {
            return Optional.empty();
        }

        Optional<UserSession> storedSession = userSessionRepository.findById(oldSessionId);
        if (storedSession.isEmpty() || !userId.equals(storedSession.get().getUserId())) {
            return Optional.empty();
        }

        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return Optional.empty();
        }

        User user = userOptional.get();
        String newSessionId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        userSessionRepository.deleteBySessionId(oldSessionId);
        UserSession refreshedSession = new UserSession(newSessionId, user.getId(), now, now.plusMinutes(30));
        userSessionRepository.save(refreshedSession);

        return Optional.of(toAuthUserResponse(user, newSessionId, true));
    }

    public Optional<CreatedUserResponse> createUserByAdmin(CreateUserRequest request) {
        if (request == null
                || isBlank(request.getUsername())
                || isBlank(request.getPassword())
                || isBlank(request.getFirstName())
                || isBlank(request.getLastName())
                || isBlank(request.getEmail())
                || request.getRoleId() == null
                || request.getBirthDate() == null) {
            return Optional.empty();
        }

        if (!isSupportedRoleId(request.getRoleId()) || userRepository.existsByUsername(request.getUsername())) {
            return Optional.empty();
        }

        Optional<Role> role = userRepository.findRoleById(request.getRoleId());
        if (role.isEmpty()) {
            return Optional.empty();
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setBirthDate(request.getBirthDate());
        // address_id is intentionally stored as NULL until address table is introduced.
        user.setAddressId(null);
        user.setRole(role.get());

        return userRepository.createUser(user).map(this::toCreatedUserResponse);
    }

    public void logout(String authorizationHeader) {
        Optional<String> sessionId = extractSessionIdFromToken(authorizationHeader);
        sessionId.ifPresent(userSessionRepository::deleteBySessionId);
    }

    public Optional<AuthUserResponse> getCurrentUserByToken(String authorizationHeader) {
        Optional<String> sessionId = extractSessionIdFromToken(authorizationHeader);
        if (sessionId.isEmpty()) {
            return Optional.empty();
        }

        Optional<UserSession> activeSession = userSessionRepository.findActiveBySessionId(sessionId.get(), LocalDateTime.now());
        if (activeSession.isEmpty()) {
            return Optional.empty();
        }

        Long userId = activeSession.get().getUserId();
        return userRepository.findById(userId).map(user -> toAuthUserResponse(user, sessionId.get(), false));
    }

    public Optional<User> authenticateSession(String authorizationHeader) {
        Optional<String> sessionId = extractSessionIdFromToken(authorizationHeader);
        if (sessionId.isEmpty()) {
            return Optional.empty();
        }

        Optional<UserSession> activeSession = userSessionRepository.findActiveBySessionId(sessionId.get(), LocalDateTime.now());
        if (activeSession.isEmpty()) {
            return Optional.empty();
        }

        return userRepository.findById(activeSession.get().getUserId());
    }

    public Optional<User> getAuthenticatedUserFromContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return Optional.of(user);
        }

        return Optional.empty();
    }

    private Optional<String> extractSessionIdFromToken(String tokenOrHeaderValue) {
        Optional<String> rawToken = extractToken(tokenOrHeaderValue);
        if (rawToken.isEmpty()) {
            return Optional.empty();
        }

        Optional<Claims> claims = jwtTokenService.parseClaims(rawToken.get());
        if (claims.isEmpty()) {
            return Optional.empty();
        }

        if (jwtTokenService.isRefreshToken(claims.get())) {
            return Optional.empty();
        }

        String sessionId = jwtTokenService.extractSessionId(claims.get());
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(sessionId);
    }

    private Optional<String> extractToken(String tokenOrHeaderValue) {
        if (tokenOrHeaderValue == null || tokenOrHeaderValue.isBlank()) {
            return Optional.empty();
        }

        String value = tokenOrHeaderValue.trim();
        if (value.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            String token = value.substring(BEARER_PREFIX.length()).trim();
            return token.isBlank() ? Optional.empty() : Optional.of(token);
        }

        return Optional.of(value);
    }

    private AuthUserResponse toAuthUserResponse(User user, String sessionId, boolean includeRefreshToken) {
        Role role = user.getRole();
        Long roleId = role != null ? role.getId() : null;
        String roleName = role != null ? role.getRoleName() : null;
        String accessToken = jwtTokenService.createAccessToken(user, sessionId);
        String refreshToken = includeRefreshToken ? jwtTokenService.createRefreshToken(user, sessionId) : null;

        return new AuthUserResponse(
                accessToken,
                refreshToken,
                sessionId,
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                roleId,
                roleName
        );
    }

    private CreatedUserResponse toCreatedUserResponse(User user) {
        Role role = user.getRole();
        return new CreatedUserResponse(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getBirthDate(),
                user.getAddressId(),
                role != null ? role.getId() : null,
                role != null ? role.getRoleName() : null
        );
    }

    private boolean isSupportedRoleId(Long roleId) {
        return STUDENT_ROLE_ID.equals(roleId) || PROFESSOR_ROLE_ID.equals(roleId) || ADMIN_ROLE_ID.equals(roleId);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}


