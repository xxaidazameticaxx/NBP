package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.dto.auth.AuthUserResponse;
import ba.unsa.etf.NBP.dto.auth.CreateUserRequest;
import ba.unsa.etf.NBP.dto.auth.CreatedUserResponse;
import ba.unsa.etf.NBP.dto.auth.LoginRequest;
import ba.unsa.etf.NBP.model.Professor;
import ba.unsa.etf.NBP.model.Role;
import ba.unsa.etf.NBP.model.Student;
import ba.unsa.etf.NBP.model.User;
import ba.unsa.etf.NBP.model.UserSession;
import ba.unsa.etf.NBP.repository.DepartmentRepository;
import ba.unsa.etf.NBP.repository.ProfessorRepository;
import ba.unsa.etf.NBP.repository.StudentRepository;
import ba.unsa.etf.NBP.repository.StudyProgramRepository;
import ba.unsa.etf.NBP.repository.UserRepository;
import ba.unsa.etf.NBP.repository.UserSessionRepository;
import ba.unsa.etf.NBP.security.JwtTokenService;
import io.jsonwebtoken.Claims;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Authentication, session management, and admin-driven user creation.
 * <p>
 * Sessions are tracked in {@code NBP_USER_SESSION} and paired with a JWT access
 * token (carrying the session ID as a claim) plus a refresh token. Clients send
 * the access token via the {@code Authorization: Bearer <token>} header.
 * Admin user creation is role-aware and can auto-provision {@code NBP_STUDENT}
 * or {@code NBP_PROFESSOR} rows together with {@code NBP.NBP_USER}.
 */
@Service
public class AuthService {

    /** HTTP header used to carry the bearer access token. */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /** Prefix for bearer token values in the {@link #AUTHORIZATION_HEADER}. */
    public static final String BEARER_PREFIX = "Bearer ";

    private static final Long STUDENT_ROLE_ID = 1L;
    private static final Long PROFESSOR_ROLE_ID = 2L;
    private static final Long ADMIN_ROLE_ID = 3L;

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final StudentRepository studentRepository;
    private final ProfessorRepository professorRepository;
    private final StudyProgramRepository studyProgramRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(UserRepository userRepository,
                       UserSessionRepository userSessionRepository,
                       StudentRepository studentRepository,
                       ProfessorRepository professorRepository,
                       StudyProgramRepository studyProgramRepository,
                       DepartmentRepository departmentRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.studentRepository = studentRepository;
        this.professorRepository = professorRepository;
        this.studyProgramRepository = studyProgramRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    /**
     * Validates credentials and issues a fresh session plus access and refresh tokens.
     * <p>
     * Any previous sessions for the user are deleted so each user holds at most
     * one active session at a time.
     *
     * @param request the login request
     * @return the user profile with new tokens, or {@link Optional#empty()} if the
     *         credentials are invalid
     */
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

    /**
     * Exchanges a valid refresh token for a new pair of access and refresh tokens
     * and rotates the underlying session ID.
     *
     * @param refreshToken the refresh token (raw or with {@code Bearer } prefix)
     * @return new tokens and user info, or {@link Optional#empty()} if the token is
     *         invalid, not a refresh token, or references an unknown user/session
     */
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

    /**
     * Creates a new user with a bcrypt-hashed password.
     * <p>
     * Rejects the request if any field is missing, the role is not one of
     * STUDENT/PROFESSOR/ADMIN, the username is already taken, or role-specific
     * fields are invalid.
     * <p>
     * Role-specific behavior:
     * <ul>
     *   <li>STUDENT ({@code roleId=1}): requires {@code indexNumber} and a valid
     *       {@code studyProgramId}; creates {@code NBP_STUDENT}</li>
     *   <li>PROFESSOR ({@code roleId=2}): requires a valid {@code departmentId};
     *       creates {@code NBP_PROFESSOR}</li>
     *   <li>ADMIN ({@code roleId=3}): creates only {@code NBP.NBP_USER} and
     *       rejects profile-specific fields</li>
     * </ul>
     * The method is transactional so user/profile inserts succeed or fail together.
     *
     * @param request the new user's details
     * @return the created user, or {@link Optional#empty()} on validation failure
     */
    @Transactional
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

        if (!hasValidRoleSpecificFields(request)) {
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

        Optional<User> createdUser = userRepository.createUser(user);
        if (createdUser.isEmpty()) {
            return Optional.empty();
        }

        User persistedUser = createdUser.get();
        if (STUDENT_ROLE_ID.equals(request.getRoleId())) {
            Student student = new Student();
            student.setUserId(persistedUser.getId());
            student.setIndexNumber(request.getIndexNumber());
            student.setStudyProgramId(request.getStudyProgramId());
            student.setEnrollmentYear(request.getEnrollmentYear());
            studentRepository.save(student);
        } else if (PROFESSOR_ROLE_ID.equals(request.getRoleId())) {
            Professor professor = new Professor();
            professor.setUserId(persistedUser.getId());
            professor.setTitle(request.getTitle());
            professor.setDepartmentId(request.getDepartmentId());
            professor.setOfficeLocation(request.getOfficeLocation());
            professorRepository.save(professor);
        }

        return Optional.of(toCreatedUserResponse(persistedUser));
    }

    /**
     * Revokes the session referenced by the given access token.
     *
     * @param authorizationHeader raw token or {@code Bearer} header value
     */
    public void logout(String authorizationHeader) {
        Optional<String> sessionId = extractSessionIdFromToken(authorizationHeader);
        sessionId.ifPresent(userSessionRepository::deleteBySessionId);
    }

    /**
     * Returns the currently authenticated user, derived from an access token.
     * <p>
     * Verifies the token, extracts the session ID, and checks the stored session
     * is still active.
     *
     * @param authorizationHeader raw token or {@code Bearer} header value
     * @return the user profile, or {@link Optional#empty()} if the token or
     *         session is invalid or expired
     */
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

    /**
     * Resolves an access token to the backing {@link User} entity.
     * <p>
     * Used by the security filter to populate the Spring Security context.
     *
     * @param authorizationHeader raw token or {@code Bearer} header value
     * @return the user, or {@link Optional#empty()} if the token or session is invalid
     */
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

    /**
     * Reads the {@link User} previously stored as the principal on the current
     * Spring Security {@link Authentication}. Controllers call this after the
     * security filter has authenticated the request.
     *
     * @return the authenticated user, or {@link Optional#empty()} if none
     */
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

    /**
     * Extracts the session ID claim from an access token.
     * <p>
     * Refresh tokens are rejected here; use {@link #refresh(String)} for those.
     *
     * @param tokenOrHeaderValue raw token or {@code Bearer} header value
     * @return the session ID, or {@link Optional#empty()} on any validation failure
     */
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

    /**
     * Strips a {@code Bearer } prefix if present and returns the raw JWT.
     *
     * @param tokenOrHeaderValue input value
     * @return the raw token string, or {@link Optional#empty()} if blank
     */
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

    /**
     * Builds the {@link AuthUserResponse} DTO, minting a fresh access token and,
     * optionally, a refresh token.
     *
     * @param user                 authenticated user
     * @param sessionId            the session ID to embed in the access token
     * @param includeRefreshToken  whether to also mint a refresh token
     * @return a response DTO with tokens and profile fields
     */
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

    /**
     * Maps a persisted {@link User} to the {@link CreatedUserResponse} returned
     * by the admin user-creation endpoint.
     *
     * @param user the created user
     * @return the response DTO
     */
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

    /**
     * Checks whether a role ID corresponds to one of STUDENT, PROFESSOR, or ADMIN.
     *
     * @param roleId the role ID
     * @return {@code true} if supported, {@code false} otherwise
     */
    private boolean isSupportedRoleId(Long roleId) {
        return STUDENT_ROLE_ID.equals(roleId) || PROFESSOR_ROLE_ID.equals(roleId) || ADMIN_ROLE_ID.equals(roleId);
    }

    /**
     * Validates role-specific payload rules and required foreign-key references.
     *
     * @param request user creation request
     * @return {@code true} when payload matches the selected role
     */
    private boolean hasValidRoleSpecificFields(CreateUserRequest request) {
        Long roleId = request.getRoleId();

        if (STUDENT_ROLE_ID.equals(roleId)) {
            return !isBlank(request.getIndexNumber())
                    && request.getStudyProgramId() != null
                    && studyProgramRepository.findById(request.getStudyProgramId()).isPresent()
                    && !hasAnyProfessorFields(request);
        }

        if (PROFESSOR_ROLE_ID.equals(roleId)) {
            return request.getDepartmentId() != null
                    && departmentRepository.findById(request.getDepartmentId()).isPresent()
                    && !hasAnyStudentFields(request);
        }

        return !hasAnyStudentFields(request) && !hasAnyProfessorFields(request);
    }

    /**
     * Checks whether any student-only fields are present.
     *
     * @param request user creation request
     * @return {@code true} if at least one student-only field is populated
     */
    private boolean hasAnyStudentFields(CreateUserRequest request) {
        return !isBlank(request.getIndexNumber())
                || request.getStudyProgramId() != null
                || request.getEnrollmentYear() != null;
    }

    /**
     * Checks whether any professor-only fields are present.
     *
     * @param request user creation request
     * @return {@code true} if at least one professor-only field is populated
     */
    private boolean hasAnyProfessorFields(CreateUserRequest request) {
        return !isBlank(request.getTitle())
                || request.getDepartmentId() != null
                || !isBlank(request.getOfficeLocation());
    }

    /**
     * Null-safe {@code String.isBlank()} check.
     *
     * @param value the string to test
     * @return {@code true} if null or blank
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
