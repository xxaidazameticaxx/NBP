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

/**
 * Attendance endpoints under {@code /attendances}.
 * <p>
 * Students register their presence by entering the 6-digit session code; professors
 * can override presence, view session attendance, and browse per-student history.
 */
@RestController
@RequestMapping("/attendances")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AuthService authService;

    public AttendanceController(AttendanceService attendanceService, AuthService authService) {
        this.attendanceService = attendanceService;
        this.authService = authService;
    }

    /**
     * Registers the caller as present using the session code the professor shared.
     *
     * @param request body with the session code
     * @return {@code 201 Created} with the attendance record
     */
    @PostMapping("/register")
    public ResponseEntity<Attendance> registerAttendance(@RequestBody RegisterAttendanceRequest request) {
        User currentUser = getAuthenticatedUser();
        String sessionCode = request != null ? request.getSessionCode() : null;
        Attendance created = attendanceService.registerAttendance(sessionCode, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Overrides the presence flag on an attendance record (professor action).
     *
     * @param id      attendance record ID
     * @param request body with the new {@code isPresent} value
     * @return the updated attendance record
     * @throws ResponseStatusException 400 if {@code isPresent} is missing
     */
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

    /**
     * Returns attendance records for one session, with student info attached.
     *
     * @param sessionId session ID
     * @return attendance rows with student details
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<SessionAttendanceRecordResponse>> getAttendanceForSession(
            @PathVariable Long sessionId) {
        User currentUser = getAuthenticatedUser();
        List<SessionAttendanceRecordResponse> records = attendanceService.getAttendanceForSession(sessionId, currentUser);
        return ResponseEntity.ok(records);
    }

    /**
     * Returns the full attendance history for a student.
     *
     * @param studentId student ID
     * @return the student's attendance records
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Attendance>> getAttendanceHistoryForStudent(
            @PathVariable Long studentId) {
        User currentUser = getAuthenticatedUser();
        List<Attendance> history = attendanceService.getAttendanceHistoryForStudent(studentId, currentUser);
        return ResponseEntity.ok(history);
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
