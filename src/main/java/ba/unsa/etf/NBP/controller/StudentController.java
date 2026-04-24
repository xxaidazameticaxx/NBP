package ba.unsa.etf.NBP.controller;

import ba.unsa.etf.NBP.dto.enrollment.StudentCourseDto;
import ba.unsa.etf.NBP.model.Attendance;
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

/**
 * Student endpoints under {@code /students}.
 * <p>
 * Besides CRUD, exposes a student's enrolled courses and attendance history.
 * Students may only view their own data; other roles may view any student's.
 */
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

    /**
     * Lists every student.
     *
     * @return all students
     */
    @GetMapping
    public List<Student> findAll() {
        return studentService.findAll();
    }

    /**
     * Returns a single student by ID.
     *
     * @param id student ID
     * @return the student, or {@code 404 Not Found}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Student> findById(@PathVariable Long id) {
        return studentService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a new student.
     *
     * @param student student payload
     * @return {@code 201 Created}
     */
    @PostMapping
    public ResponseEntity<Void> save(@RequestBody Student student) {
        studentService.save(student);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Updates an existing student.
     *
     * @param id      student ID
     * @param student updated fields
     * @return {@code 200 OK}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody Student student) {
        student.setId(id);
        studentService.update(student);
        return ResponseEntity.ok().build();
    }

    /**
     * Deletes a student.
     *
     * @param id student ID
     * @return {@code 204 No Content}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        studentService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Returns the courses a student is enrolled in, with full course details.
     * Students may only view their own courses.
     *
     * @param studentId target student ID
     * @return the enrolled courses
     * @throws ResponseStatusException 404 if student not found, 403 if forbidden
     */
    @GetMapping("/{studentId}/courses")
    public ResponseEntity<List<StudentCourseDto>> getEnrolledCourses(@PathVariable Long studentId) {

        User currentUser = authService.getAuthenticatedUserFromContext()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

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

    /**
     * Returns the full attendance history for a student.
     * Students may only view their own records.
     *
     * @param studentId target student ID
     * @return the student's attendance records
     * @throws ResponseStatusException 404 if student not found, 403 if forbidden
     */
    @GetMapping("/{studentId}/attendance")
    public ResponseEntity<List<Attendance>> getStudentAttendance(@PathVariable Long studentId) {

        User currentUser = authService.getAuthenticatedUserFromContext()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

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
