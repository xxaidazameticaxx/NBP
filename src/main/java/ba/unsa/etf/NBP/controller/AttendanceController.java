package ba.unsa.etf.NBP.controller;

import ba.unsa.etf.NBP.model.Attendance;
import ba.unsa.etf.NBP.service.AttendanceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/attendances")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @GetMapping
    public List<Attendance> findAll() {
        return attendanceService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Attendance> findById(@PathVariable Long id) {
        return attendanceService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Void> save(@RequestBody Attendance attendance) {
        attendanceService.save(attendance);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody Attendance attendance) {
        attendance.setId(id);
        attendanceService.update(attendance);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        attendanceService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Attendance>> findByStudentId(@PathVariable Long studentId) {
        List<Attendance> attendances = attendanceService.findByStudentId(studentId);
        return ResponseEntity.ok(attendances);
    }

    @GetMapping("/course-session/{courseSessionId}")
    public ResponseEntity<List<Attendance>> findByCourseSessionId(@PathVariable Long courseSessionId) {
        List<Attendance> attendances = attendanceService.findByCourseSessionId(courseSessionId);
        return ResponseEntity.ok(attendances);
    }
}
