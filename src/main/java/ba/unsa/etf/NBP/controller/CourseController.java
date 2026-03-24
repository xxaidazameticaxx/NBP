package ba.unsa.etf.NBP.controller;

import ba.unsa.etf.NBP.model.Course;
import ba.unsa.etf.NBP.service.CourseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
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
}
