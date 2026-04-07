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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    public static final String SESSION_HEADER = "X-Session-Id";
    private static final Long STUDENT_ROLE_ID = 1L;
    private static final Long PROFESSOR_ROLE_ID = 2L;
    private static final Long ADMIN_ROLE_ID = 3L;

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       UserSessionRepository userSessionRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordEncoder = passwordEncoder;
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

        return Optional.of(toAuthUserResponse(user, newSessionId));
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

    public void logout(String sessionId) {
        userSessionRepository.deleteBySessionId(sessionId);
    }

    public Optional<AuthUserResponse> getCurrentUserBySession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }

        Optional<UserSession> activeSession = userSessionRepository.findActiveBySessionId(sessionId, LocalDateTime.now());
        if (activeSession.isEmpty()) {
            return Optional.empty();
        }

        Long userId = activeSession.get().getUserId();
        return userRepository.findById(userId).map(user -> toAuthUserResponse(user, sessionId));
    }

    public Optional<User> authenticateSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }

        Optional<UserSession> activeSession = userSessionRepository.findActiveBySessionId(sessionId, LocalDateTime.now());
        if (activeSession.isEmpty()) {
            return Optional.empty();
        }

        return userRepository.findById(activeSession.get().getUserId());
    }

    private AuthUserResponse toAuthUserResponse(User user, String sessionId) {
        Role role = user.getRole();
        Long roleId = role != null ? role.getId() : null;
        String roleName = role != null ? role.getRoleName() : null;

        return new AuthUserResponse(
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


