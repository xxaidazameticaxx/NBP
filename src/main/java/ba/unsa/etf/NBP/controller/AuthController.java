package ba.unsa.etf.NBP.controller;

import ba.unsa.etf.NBP.dto.auth.AuthUserResponse;
import ba.unsa.etf.NBP.dto.auth.CreateUserRequest;
import ba.unsa.etf.NBP.dto.auth.CreatedUserResponse;
import ba.unsa.etf.NBP.dto.auth.LoginRequest;
import ba.unsa.etf.NBP.dto.auth.RefreshTokenRequest;
import ba.unsa.etf.NBP.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * Authentication endpoints under {@code /auth}.
 * <p>
 * The application uses JWT tokens: login issues a short-lived access token and a
 * refresh token. Clients send the access token as {@code Authorization: Bearer <token>}
 * on every protected endpoint.
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Authenticates a user and issues JWT access and refresh tokens.
     *
     * @param request username and password
     * @return {@code 200 OK} with tokens and profile, or {@code 401 Unauthorized}
     */
    @PostMapping("/login")
    @Operation(summary = "Login and issue access/refresh JWT tokens", security = {})
    public ResponseEntity<AuthUserResponse> login(@RequestBody LoginRequest request) {
        Optional<AuthUserResponse> response = authService.login(request);
        return response.map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    /**
     * Revokes the current session by invalidating the access token.
     *
     * @param authorizationHeader {@code Authorization} header with the bearer token
     * @return {@code 204 No Content}, or {@code 401 Unauthorized} if header missing
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout and revoke active session", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> logout(@RequestHeader(value = AuthService.AUTHORIZATION_HEADER, required = false) String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        authService.logout(authorizationHeader);
        return ResponseEntity.noContent().build();
    }

    /**
     * Exchanges a valid refresh token for a new pair of access and refresh tokens.
     *
     * @param request the refresh token payload
     * @return {@code 200 OK} with new tokens, or {@code 401 Unauthorized}
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token", security = {})
    public ResponseEntity<AuthUserResponse> refresh(@RequestBody RefreshTokenRequest request) {
        if (request == null || request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return authService.refresh(request.getRefreshToken())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    /**
     * Returns the profile of the currently authenticated user.
     *
     * @param authorizationHeader {@code Authorization} header with the bearer token
     * @return {@code 200 OK} with user info, or {@code 401 Unauthorized}
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user profile from access token", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<AuthUserResponse> me(@RequestHeader(value = AuthService.AUTHORIZATION_HEADER, required = false) String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return authService.getCurrentUserByToken(authorizationHeader)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    /**
     * Creates a new user account. Admin only.
     * <p>
     * The payload is role-aware: STUDENT users require student profile fields,
     * PROFESSOR users require professor profile fields, and ADMIN users must not
     * include profile-specific fields. On success, the service also creates the
     * corresponding {@code NBP_STUDENT} or {@code NBP_PROFESSOR} row.
     *
     * @param request new user's details (username, password, role, etc.)
     * @return {@code 201 Created} on success, or {@code 400 Bad Request} if
     *         validation fails (including role/profile mismatch or bad FK values)
     */
    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create user account (admin only)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<CreatedUserResponse> createUser(@RequestBody CreateUserRequest request) {
        return authService.createUserByAdmin(request)
                .map(created -> ResponseEntity.status(HttpStatus.CREATED).body(created))
                .orElse(ResponseEntity.badRequest().build());
    }
}
