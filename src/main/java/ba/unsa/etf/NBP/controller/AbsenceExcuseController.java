package ba.unsa.etf.NBP.controller;

import ba.unsa.etf.NBP.dto.excuse.SubmitExcuseRequest;
import ba.unsa.etf.NBP.model.AbsenceExcuse;
import ba.unsa.etf.NBP.model.User;
import ba.unsa.etf.NBP.service.AbsenceExcuseService;
import ba.unsa.etf.NBP.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/absence-excuses")
public class AbsenceExcuseController {

    private final AbsenceExcuseService absenceExcuseService;
    private final AuthService authService;

    public AbsenceExcuseController(AbsenceExcuseService absenceExcuseService, AuthService authService) {
        this.absenceExcuseService = absenceExcuseService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<AbsenceExcuse> submitExcuse(@RequestBody SubmitExcuseRequest request) {
        User currentUser = getAuthenticatedUser();
        AbsenceExcuse excuse = absenceExcuseService.submitExcuse(
                request.courseSessionId(), request.reason(), currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(excuse);
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<Void> approveExcuse(@PathVariable Long id) {
        User currentUser = getAuthenticatedUser();
        absenceExcuseService.approveExcuse(id, currentUser);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<Void> rejectExcuse(@PathVariable Long id) {
        User currentUser = getAuthenticatedUser();
        absenceExcuseService.rejectExcuse(id, currentUser);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<AbsenceExcuse>> findByStudentId(@PathVariable Long studentId) {
        return ResponseEntity.ok(absenceExcuseService.findByStudentId(studentId));
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<AbsenceExcuse>> findBySessionId(@PathVariable Long sessionId) {
        return ResponseEntity.ok(absenceExcuseService.findByCourseSessionId(sessionId));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<AbsenceExcuse>> findPending() {
        User currentUser = getAuthenticatedUser();
        return ResponseEntity.ok(absenceExcuseService.findPendingByProfessor(currentUser));
    }

    private User getAuthenticatedUser() {
        return authService.getAuthenticatedUserFromContext()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
    }
}
