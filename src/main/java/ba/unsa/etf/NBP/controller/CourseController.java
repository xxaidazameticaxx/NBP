package ba.unsa.etf.NBP.controller;

import ba.unsa.etf.NBP.dto.session.CourseSessionResponse;
import ba.unsa.etf.NBP.dto.enrollment.EnrolledStudentDto;
import ba.unsa.etf.NBP.model.Course;
import ba.unsa.etf.NBP.model.Professor;
import ba.unsa.etf.NBP.model.User;
import ba.unsa.etf.NBP.service.AuthService;
import ba.unsa.etf.NBP.service.CourseService;
import ba.unsa.etf.NBP.service.CourseSessionService;
import ba.unsa.etf.NBP.service.EnrollmentService;
import ba.unsa.etf.NBP.service.ProfessorService;
import ba.unsa.etf.NBP.service.StudentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Course endpoints under {@code /courses}: CRUD, lookups by professor or department,
 * attendance-session history, and class roster.
 */
@RestController
@RequestMapping("/courses")
public class CourseController {

    private final CourseService courseService;
    private final CourseSessionService courseSessionService;
    private final AuthService authService;
    private final EnrollmentService enrollmentService;
    private final ProfessorService professorService;
    private final StudentService studentService;

    public CourseController(CourseService courseService,
                            CourseSessionService courseSessionService,
                            EnrollmentService enrollmentService,
                            ProfessorService professorService,
                            AuthService authService,
                            StudentService studentService)
    {
        this.courseService = courseService;
        this.courseSessionService = courseSessionService;
        this.authService = authService;
        this.enrollmentService = enrollmentService;
        this.professorService = professorService;
        this.studentService = studentService;
    }

    /**
     * Lists every course.
     *
     * @return all courses
     */
    @GetMapping
    public List<Course> findAll() {
        return courseService.findAll();
    }

    /**
     * Returns a single course by ID.
     *
     * @param id course ID
     * @return the course, or {@code 404 Not Found}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Course> findById(@PathVariable Long id) {
        return courseService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a new course.
     *
     * @param course course payload
     * @return {@code 201 Created}
     */
    @PostMapping
    public ResponseEntity<Void> save(@RequestBody Course course) {
        courseService.save(course);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Updates an existing course.
     *
     * @param id     course ID
     * @param course updated fields
     * @return {@code 200 OK}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody Course course) {
        course.setId(id);
        courseService.update(course);
        return ResponseEntity.ok().build();
    }

    /**
     * Deletes a course.
     *
     * @param id course ID
     * @return {@code 204 No Content}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        courseService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Lists all courses taught by a specific professor.
     *
     * @param professorId professor ID
     * @return courses taught by that professor
     */
    @GetMapping("/professor/{professorId}")
    public ResponseEntity<List<Course>> findByProfessorId(@PathVariable Long professorId) {
        List<Course> courses = courseService.findByProfessorId(professorId);
        return ResponseEntity.ok(courses);
    }

    /**
     * Lists all courses offered by a specific department.
     *
     * @param departmentId department ID
     * @return courses in that department
     */
    @GetMapping("/department/{departmentId}")
    public ResponseEntity<List<Course>> findByDepartmentId(@PathVariable Long departmentId) {
        List<Course> courses = courseService.findByDepartmentId(departmentId);
        return ResponseEntity.ok(courses);
    }

    /**
     * Returns the attendance-session history for a course, ordered by start time.
     *
     * @param courseId course ID
     * @return the session history
     */
    @GetMapping("/{courseId}/sessions")
    public ResponseEntity<List<CourseSessionResponse>> getCourseSessionHistory(@PathVariable Long courseId) {
        User currentUser = authService.getAuthenticatedUserFromContext()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        List<CourseSessionResponse> sessions = courseSessionService.getCourseSessionHistory(courseId, currentUser);
        return ResponseEntity.ok(sessions);
    }

    /**
     * Returns the roster of students enrolled in a course.
     * <p>
     * Accessible to administrators (any course) and professors (only for courses they own).
     *
     * @param courseId course ID
     * @return the enrolled students
     */
    @GetMapping("/{courseId}/students")
    public ResponseEntity<List<EnrolledStudentDto>> getClassRoster(@PathVariable Long courseId) {

        User currentUser = authService.getAuthenticatedUserFromContext()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

        Course course = courseService.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        boolean isAdmin = currentUser.getRole() != null && currentUser.getRole().getId().equals(3L);

        if (!isAdmin) {
            Professor professor = professorService.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Only professors and administrators can view class rosters"));
            if (!course.getProfessorId().equals(professor.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to view this roster (You do not own this course)");
            }
        }

        List<EnrolledStudentDto> roster = studentService.findRosterByCourseId(courseId);
        return ResponseEntity.ok(roster);
    }
}
