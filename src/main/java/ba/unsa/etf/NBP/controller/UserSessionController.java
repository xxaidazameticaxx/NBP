package ba.unsa.etf.NBP.controller;

import ba.unsa.etf.NBP.model.UserSession;
import ba.unsa.etf.NBP.service.UserSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD endpoints over the {@code NBP_USER_SESSION} table under {@code /user-sessions}.
 * <p>
 * Used for administration and diagnostics; regular login/logout goes through
 * {@link AuthController} instead.
 */
@RestController
@RequestMapping("/user-sessions")
public class UserSessionController {

    private final UserSessionService userSessionService;

    public UserSessionController(UserSessionService userSessionService) {
        this.userSessionService = userSessionService;
    }

    /**
     * Lists every stored session.
     *
     * @return all sessions
     */
    @GetMapping
    public List<UserSession> findAll() {
        return userSessionService.findAll();
    }

    /**
     * Returns one session by its ID.
     *
     * @param sessionId session ID
     * @return the session, or {@code 404 Not Found}
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<UserSession> findById(@PathVariable String sessionId) {
        return userSessionService.findById(sessionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Lists every session belonging to a user.
     *
     * @param userId user ID
     * @return sessions for that user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserSession>> findByUserId(@PathVariable Long userId) {
        List<UserSession> sessions = userSessionService.findByUserId(userId);
        return ResponseEntity.ok(sessions);
    }

    /**
     * Stores a session row directly.
     *
     * @param userSession session payload
     * @return {@code 201 Created}
     */
    @PostMapping
    public ResponseEntity<Void> save(@RequestBody UserSession userSession) {
        userSessionService.save(userSession);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Updates a session row.
     *
     * @param sessionId   session ID from the path
     * @param userSession updated fields
     * @return {@code 200 OK}
     */
    @PutMapping("/{sessionId}")
    public ResponseEntity<Void> update(@PathVariable String sessionId, @RequestBody UserSession userSession) {
        userSession.setSessionId(sessionId);
        userSessionService.update(userSession);
        return ResponseEntity.ok().build();
    }

    /**
     * Deletes one session by ID.
     *
     * @param sessionId session ID
     * @return {@code 204 No Content}
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> deleteBySessionId(@PathVariable String sessionId) {
        userSessionService.deleteBySessionId(sessionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Deletes every session belonging to a user.
     *
     * @param userId user ID
     * @return {@code 204 No Content}
     */
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> deleteByUserId(@PathVariable Long userId) {
        userSessionService.deleteById(userId);
        return ResponseEntity.noContent().build();
    }
}
