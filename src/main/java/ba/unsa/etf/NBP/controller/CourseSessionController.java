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

@RestController
@RequestMapping("/course-sessions")
public class CourseSessionController {

    private final CourseSessionService courseSessionService;
    private final AuthService authService;

    public CourseSessionController(CourseSessionService courseSessionService, AuthService authService) {
        this.courseSessionService = courseSessionService;
        this.authService = authService;
    }

    @GetMapping
    public List<CourseSession> findAll() {
        return courseSessionService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseSession> findById(@PathVariable Long id) {
        return courseSessionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Void> save(@RequestBody CourseSession courseSession) {
        courseSessionService.save(courseSession);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody CourseSession courseSession) {
        courseSession.setId(id);
        courseSessionService.update(courseSession);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        courseSessionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{courseId}/open")
    public ResponseEntity<CourseSessionResponse> openSession(
            @PathVariable Long courseId,
            @RequestBody(required = false) OpenSessionRequest request,
            @RequestHeader(AuthService.SESSION_HEADER) String sessionId) {
        User currentUser = getAuthenticatedUser(sessionId);
        CourseSessionResponse response = courseSessionService.openSession(courseId, request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}/close")
    public ResponseEntity<CourseSessionResponse> closeSession(
            @PathVariable Long id,
            @RequestHeader(AuthService.SESSION_HEADER) String sessionId) {
        User currentUser = getAuthenticatedUser(sessionId);
        CourseSessionResponse response = courseSessionService.closeSession(id, currentUser);
        return ResponseEntity.ok(response);
    }

private User getAuthenticatedUser(String sessionId) {
        return authService.authenticateSession(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid session"));
    }
}
