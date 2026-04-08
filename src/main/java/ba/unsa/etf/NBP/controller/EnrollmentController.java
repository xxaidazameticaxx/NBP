package ba.unsa.etf.NBP.controller;

import ba.unsa.etf.NBP.model.Enrollment;
import ba.unsa.etf.NBP.model.User;
import ba.unsa.etf.NBP.service.AuthService;
import ba.unsa.etf.NBP.service.CourseService;
import ba.unsa.etf.NBP.service.EnrollmentService;
import ba.unsa.etf.NBP.service.StudentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final StudentService studentService;
    private final CourseService courseService;
    private final AuthService authService;

    public EnrollmentController(EnrollmentService enrollmentService,
                                StudentService studentService,
                                CourseService courseService,
                                AuthService authService) {
        this.enrollmentService = enrollmentService;
        this.studentService = studentService;
        this.courseService = courseService;
        this.authService = authService;
    }

    @GetMapping
    public List<Enrollment> findAll() {
        return enrollmentService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Enrollment> findById(@PathVariable Long id) {
        return enrollmentService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Void> save(@RequestBody Enrollment enrollment) {

        User currentUser = authService.getAuthenticatedUserFromContext()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

        if (currentUser.getRole() == null || currentUser.getRole().getId() != 3L) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only administrators can enroll students");
        }

        if (courseService.findById(enrollment.getCourseId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }

        if (studentService.findById(enrollment.getStudentId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found");
        }

        if (enrollmentService.existsByStudentAndCourse(enrollment.getStudentId(), enrollment.getCourseId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student is already enrolled in this course");
        }

        enrollment.setEnrollmentDate(LocalDate.now());
        enrollmentService.save(enrollment);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody Enrollment enrollment) {
        enrollment.setId(id);
        enrollmentService.update(enrollment);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {

        User currentUser = authService.getAuthenticatedUserFromContext()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

        if (currentUser.getRole() == null || currentUser.getRole().getId() != 3L) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only administrators can unenroll students");
        }

        enrollmentService.deleteById(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Enrollment>> findByStudentId(@PathVariable Long studentId) {
        List<Enrollment> enrollments = enrollmentService.findByStudentId(studentId);
        return ResponseEntity.ok(enrollments);
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<Enrollment>> findByCourseId(@PathVariable Long courseId) {
        List<Enrollment> enrollments = enrollmentService.findByCourseId(courseId);
        return ResponseEntity.ok(enrollments);
    }
}