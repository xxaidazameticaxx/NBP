package ba.unsa.etf.NBP.controller;

import ba.unsa.etf.NBP.dto.enrollment.EnrolledStudentDto;
import ba.unsa.etf.NBP.model.Course;
import ba.unsa.etf.NBP.model.Professor;
import ba.unsa.etf.NBP.model.User;
import ba.unsa.etf.NBP.service.AuthService;
import ba.unsa.etf.NBP.service.CourseService;
import ba.unsa.etf.NBP.service.EnrollmentService;
import ba.unsa.etf.NBP.service.ProfessorService;
import ba.unsa.etf.NBP.service.StudentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/courses")
public class CourseController {

    private final CourseService courseService;
    private final EnrollmentService enrollmentService;
    private final ProfessorService professorService;
    private final AuthService authService;
    private final StudentService studentService;

    public CourseController(CourseService courseService,
                            EnrollmentService enrollmentService,
                            ProfessorService professorService,
                            AuthService authService,
                            StudentService studentService) {
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
        this.professorService = professorService;
        this.authService = authService;
        this.studentService = studentService;
    }

    @GetMapping
    public List<Course> findAll() {
        return courseService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Course> findById(@PathVariable Long id) {
        return courseService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Void> save(@RequestBody Course course) {
        courseService.save(course);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody Course course) {
        course.setId(id);
        courseService.update(course);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        courseService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/professor/{professorId}")
    public ResponseEntity<List<Course>> findByProfessorId(@PathVariable Long professorId) {
        List<Course> courses = courseService.findByProfessorId(professorId);
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/department/{departmentId}")
    public ResponseEntity<List<Course>> findByDepartmentId(@PathVariable Long departmentId) {
        List<Course> courses = courseService.findByDepartmentId(departmentId);
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/{courseId}/students")
    public ResponseEntity<List<EnrolledStudentDto>> getClassRoster(
            @PathVariable Long courseId,
            @RequestHeader(name = AuthService.SESSION_HEADER, required = false) String sessionId) {

        User currentUser = authService.authenticateSession(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired session"));

        Course course = courseService.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        boolean isAdmin = currentUser.getRole() != null && currentUser.getRole().getId().equals(3L);

        if (!isAdmin) {
            Professor professor = professorService.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Only professors and administrators can view class rosters"));
            // Verifying if the professor owns the course
            if (!course.getProfessorId().equals(professor.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to view this roster (You do not own this course)");
            }
        }

        List<EnrolledStudentDto> roster = studentService.findRosterByCourseId(courseId);
        return ResponseEntity.ok(roster);
    }
}