package ba.unsa.etf.NBP.controller;

import ba.unsa.etf.NBP.dto.session.CourseSessionResponse;
import ba.unsa.etf.NBP.model.Course;
import ba.unsa.etf.NBP.model.User;
import ba.unsa.etf.NBP.service.AuthService;
import ba.unsa.etf.NBP.service.CourseService;
import ba.unsa.etf.NBP.service.CourseSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/courses")
public class CourseController {

    private final CourseService courseService;
    private final CourseSessionService courseSessionService;
    private final AuthService authService;

    public CourseController(CourseService courseService, CourseSessionService courseSessionService, AuthService authService) {
        this.courseService = courseService;
        this.courseSessionService = courseSessionService;
        this.authService = authService;
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

    @GetMapping("/{courseId}/sessions")
    public ResponseEntity<List<CourseSessionResponse>> getCourseSessionHistory(
            @PathVariable Long courseId,
            @RequestHeader(AuthService.SESSION_HEADER) String sessionId) {
        User currentUser = authService.authenticateSession(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid session"));
        List<CourseSessionResponse> sessions = courseSessionService.getCourseSessionHistory(courseId, currentUser);
        return ResponseEntity.ok(sessions);
    }
}
