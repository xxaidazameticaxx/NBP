package ba.unsa.etf.NBP.controller;

import ba.unsa.etf.NBP.model.Timetable;
import ba.unsa.etf.NBP.service.TimetableService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/timetables")
public class TimetableController {

    private final TimetableService timetableService;

    public TimetableController(TimetableService timetableService) {
        this.timetableService = timetableService;
    }

    @GetMapping
    public List<Timetable> findAll() {
        return timetableService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Timetable> findById(@PathVariable Long id) {
        return timetableService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Void> save(@RequestBody Timetable timetable) {
        timetableService.save(timetable);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody Timetable timetable) {
        timetable.setId(id);
        timetableService.update(timetable);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        timetableService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}