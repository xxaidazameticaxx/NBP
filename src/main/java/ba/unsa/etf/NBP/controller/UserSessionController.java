package ba.unsa.etf.NBP.controller;

import ba.unsa.etf.NBP.model.UserSession;
import ba.unsa.etf.NBP.service.UserSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user-sessions")
public class UserSessionController {

    private final UserSessionService userSessionService;

    public UserSessionController(UserSessionService userSessionService) {
        this.userSessionService = userSessionService;
    }

    @GetMapping
    public List<UserSession> findAll() {
        return userSessionService.findAll();
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<UserSession> findById(@PathVariable String sessionId) {
        return userSessionService.findById(sessionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserSession>> findByUserId(@PathVariable Long userId) {
        List<UserSession> sessions = userSessionService.findByUserId(userId);
        return ResponseEntity.ok(sessions);
    }

    @PostMapping
    public ResponseEntity<Void> save(@RequestBody UserSession userSession) {
        userSessionService.save(userSession);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{sessionId}")
    public ResponseEntity<Void> update(@PathVariable String sessionId, @RequestBody UserSession userSession) {
        userSession.setSessionId(sessionId);
        userSessionService.update(userSession);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> deleteBySessionId(@PathVariable String sessionId) {
        userSessionService.deleteBySessionId(sessionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> deleteByUserId(@PathVariable Long userId) {
        userSessionService.deleteById(userId);
        return ResponseEntity.noContent().build();
    }
}