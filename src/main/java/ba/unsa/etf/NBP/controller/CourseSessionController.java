package ba.unsa.etf.NBP.controller;

import ba.unsa.etf.NBP.dto.session.CourseSessionResponse;
import ba.unsa.etf.NBP.dto.session.OpenSessionRequest;
import ba.unsa.etf.NBP.model.CourseSession;
import ba.unsa.etf.NBP.model.User;
import ba.unsa.etf.NBP.service.AuthService;
import ba.unsa.etf.NBP.service.CourseSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Attendance-session endpoints under {@code /course-sessions}.
 * <p>
 * Professors open a session to start accepting student check-ins with a
 * generated 6-digit code, and close it when the class ends.
 */
@RestController
@RequestMapping("/course-sessions")
public class CourseSessionController {

    private final CourseSessionService courseSessionService;
    private final AuthService authService;

    public CourseSessionController(CourseSessionService courseSessionService, AuthService authService) {
        this.courseSessionService = courseSessionService;
        this.authService = authService;
    }

    /**
     * Lists every course session.
     *
     * @return all sessions
     */
    @GetMapping
    public List<CourseSession> findAll() {
        return courseSessionService.findAll();
    }

    /**
     * Returns a single session by ID.
     *
     * @param id session ID
     * @return the session, or {@code 404 Not Found}
     */
    @GetMapping("/{id}")
    public ResponseEntity<CourseSession> findById(@PathVariable Long id) {
        return courseSessionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a session directly (low-level). For the normal open-session flow, use
     * {@link #openSession(Long, OpenSessionRequest)} instead.
     *
     * @param courseSession session payload
     * @return {@code 201 Created}
     */
    @PostMapping
    public ResponseEntity<Void> save(@RequestBody CourseSession courseSession) {
        courseSessionService.save(courseSession);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Updates an existing session.
     *
     * @param id            session ID
     * @param courseSession updated fields
     * @return {@code 200 OK}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody CourseSession courseSession) {
        courseSession.setId(id);
        courseSessionService.update(courseSession);
        return ResponseEntity.ok().build();
    }

    /**
     * Deletes a session.
     *
     * @param id session ID
     * @return {@code 204 No Content}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        courseSessionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Opens a new attendance session on a course the caller owns.
     * Generates a unique 6-digit session code.
     *
     * @param courseId course the session belongs to
     * @param request  optional body specifying {@code timetableId} or {@code roomId}
     * @return {@code 201 Created} with the new session
     */
    @PostMapping("/{courseId}/open")
    public ResponseEntity<CourseSessionResponse> openSession(
            @PathVariable Long courseId,
            @RequestBody(required = false) OpenSessionRequest request) {
        User currentUser = getAuthenticatedUser();
        CourseSessionResponse response = courseSessionService.openSession(courseId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Closes an open attendance session.
     *
     * @param id session ID
     * @return {@code 200 OK} with the updated session
     */
    @PutMapping("/{id}/close")
    public ResponseEntity<CourseSessionResponse> closeSession(@PathVariable Long id) {
        User currentUser = getAuthenticatedUser();
        CourseSessionResponse response = courseSessionService.closeSession(id, currentUser);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns the authenticated user from the security context, or 401 if none.
     *
     * @return the authenticated {@link User}
     */
    private User getAuthenticatedUser() {
        return authService.getAuthenticatedUserFromContext()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
    }
}
