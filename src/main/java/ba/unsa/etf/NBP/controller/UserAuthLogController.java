package ba.unsa.etf.NBP.controller;

import ba.unsa.etf.NBP.dto.authlog.UserAuthLogDto;
import ba.unsa.etf.NBP.dto.authlog.UserAuthLogsResponse;
import ba.unsa.etf.NBP.model.User;
import ba.unsa.etf.NBP.model.UserAuthLog;
import ba.unsa.etf.NBP.repository.UserRepository;
import ba.unsa.etf.NBP.service.AuthService;
import ba.unsa.etf.NBP.service.AuthLogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * REST endpoints for querying user authentication logs.
 * <p>
 * Access is limited to admins or the user themselves.
 */
@RestController
@RequestMapping("/users")
public class UserAuthLogController {

    private static final Long ADMIN_ROLE_ID = 3L;

    private final AuthService authService;
    private final UserRepository userRepository;
    private final AuthLogService authLogService;

    public UserAuthLogController(AuthService authService, UserRepository userRepository, AuthLogService authLogService) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.authLogService = authLogService;
    }

    /**
     * Returns user details (from Oracle) together with auth logs (from MongoDB).
     */
    @GetMapping("/{userId}/auth-logs")
    public ResponseEntity<UserAuthLogsResponse> getAuthLogsForUser(@PathVariable Long userId) {

        User currentUser = authService.getAuthenticatedUserFromContext()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

        boolean isAdmin = currentUser.getRole() != null && ADMIN_ROLE_ID.equals(currentUser.getRole().getId());
        boolean isSelf = userId != null && userId.equals(currentUser.getId());
        if (!isAdmin && !isSelf) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
        }

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<UserAuthLog> logs = authLogService.getLogsForUser(userId);
        List<UserAuthLogDto> dtoLogs = logs.stream()
                .map(log -> new UserAuthLogDto(log.getTimestamp(), log.getAction()))
                .toList();

        UserAuthLogsResponse response = new UserAuthLogsResponse();
        response.setUserId(targetUser.getId());
        response.setUsername(targetUser.getUsername());
        response.setFirstName(targetUser.getFirstName());
        response.setLastName(targetUser.getLastName());
        response.setEmail(targetUser.getEmail());
        response.setLogs(dtoLogs);

        return ResponseEntity.ok(response);
    }
}
