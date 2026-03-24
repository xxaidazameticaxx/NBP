package ba.unsa.etf.NBP.controller;

import ba.unsa.etf.NBP.model.Enrollment;
import ba.unsa.etf.NBP.service.EnrollmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
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
