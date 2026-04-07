package ba.unsa.etf.NBP.controller;

import ba.unsa.etf.NBP.dto.enrollment.StudentCourseDto;
import ba.unsa.etf.NBP.model.Attendance;
import ba.unsa.etf.NBP.model.Enrollment;
import ba.unsa.etf.NBP.model.Student;
import ba.unsa.etf.NBP.model.User;
import ba.unsa.etf.NBP.service.AttendanceService;
import ba.unsa.etf.NBP.service.AuthService;
import ba.unsa.etf.NBP.service.EnrollmentService;
import ba.unsa.etf.NBP.service.StudentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/students")
public class StudentController {

    private final StudentService studentService;
    private final EnrollmentService enrollmentService;
    private final AttendanceService attendanceService;
    private final AuthService authService;

    public StudentController(StudentService studentService, EnrollmentService enrollmentService, AttendanceService attendanceService, AuthService authService) {
        this.studentService = studentService;
        this.enrollmentService = enrollmentService;
        this.attendanceService = attendanceService;
        this.authService = authService;
    }

    @GetMapping
    public List<Student> findAll() {
        return studentService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Student> findById(@PathVariable Long id) {
        return studentService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Void> save(@RequestBody Student student) {
        studentService.save(student);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody Student student) {
        student.setId(id);
        studentService.update(student);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        studentService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{studentId}/courses")
    public ResponseEntity<List<StudentCourseDto>> getEnrolledCourses(
            @PathVariable Long studentId,
            @RequestHeader(name = AuthService.SESSION_HEADER, required = false) String sessionId) {

        User currentUser = authService.authenticateSession(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired session"));

        Student targetStudent = studentService.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        if (currentUser.getRole() != null && currentUser.getRole().getId().equals(1L)) {
            if (!targetStudent.getUserId().equals(currentUser.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to view another student's courses");
            }
        }

        List<StudentCourseDto> courses = enrollmentService.findCoursesByStudentIdWithDetails(studentId);
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/{studentId}/attendance")
    public ResponseEntity<List<Attendance>> getStudentAttendance(
            @PathVariable Long studentId,
            @RequestHeader(name = AuthService.SESSION_HEADER, required = false) String sessionId) {

        User currentUser = authService.authenticateSession(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired session"));

        Student targetStudent = studentService.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        if (currentUser.getRole() != null && currentUser.getRole().getId().equals(1L)) {
            if (!targetStudent.getUserId().equals(currentUser.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to view another student's attendance records");
            }
        }

        List<Attendance> attendance = attendanceService.findByStudentId(studentId);
        return ResponseEntity.ok(attendance);
    }
}