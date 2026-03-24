package ba.unsa.etf.NBP.controller;

import ba.unsa.etf.NBP.model.CourseSession;
import ba.unsa.etf.NBP.service.CourseSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/course-sessions")
public class CourseSessionController {

    private final CourseSessionService courseSessionService;

    public CourseSessionController(CourseSessionService courseSessionService) {
        this.courseSessionService = courseSessionService;
    }

    @GetMapping
    public List<CourseSession> findAll() {
        return courseSessionService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseSession> findById(@PathVariable Long id) {
        return courseSessionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Void> save(@RequestBody CourseSession courseSession) {
        courseSessionService.save(courseSession);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody CourseSession courseSession) {
        courseSession.setId(id);
        courseSessionService.update(courseSession);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        courseSessionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
