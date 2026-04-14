package ba.unsa.etf.NBP.controller;

import ba.unsa.etf.NBP.dto.attendance.RegisterAttendanceRequest;
import ba.unsa.etf.NBP.dto.attendance.SessionAttendanceRecordResponse;
import ba.unsa.etf.NBP.dto.attendance.UpdateAttendanceRequest;
import ba.unsa.etf.NBP.model.Attendance;
import ba.unsa.etf.NBP.model.User;
import ba.unsa.etf.NBP.service.AuthService;
import ba.unsa.etf.NBP.service.AttendanceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/attendances")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AuthService authService;

    public AttendanceController(AttendanceService attendanceService, AuthService authService) {
        this.attendanceService = attendanceService;
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Attendance> registerAttendance(@RequestBody RegisterAttendanceRequest request) {
        User currentUser = getAuthenticatedUser();
        String sessionCode = request != null ? request.getSessionCode() : null;
        Attendance created = attendanceService.registerAttendance(sessionCode, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Attendance> overrideAttendance(
            @PathVariable Long id,
            @RequestBody UpdateAttendanceRequest request) {
        User currentUser = getAuthenticatedUser();
        if (request == null || request.getIsPresent() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "isPresent is required");
        }
        Attendance updated = attendanceService.overrideAttendancePresence(id, request.getIsPresent(), currentUser);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<SessionAttendanceRecordResponse>> getAttendanceForSession(
            @PathVariable Long sessionId) {
        User currentUser = getAuthenticatedUser();
        List<SessionAttendanceRecordResponse> records = attendanceService.getAttendanceForSession(sessionId, currentUser);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Attendance>> getAttendanceHistoryForStudent(
            @PathVariable Long studentId) {
        User currentUser = getAuthenticatedUser();
        List<Attendance> history = attendanceService.getAttendanceHistoryForStudent(studentId, currentUser);
        return ResponseEntity.ok(history);
    }

    private User getAuthenticatedUser() {
        return authService.getAuthenticatedUserFromContext()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
    }
}
