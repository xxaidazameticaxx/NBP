package ba.unsa.etf.NBP.controller;

import ba.unsa.etf.NBP.model.Timetable;
import ba.unsa.etf.NBP.service.TimetableService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD endpoints for scheduled class entries under {@code /timetables}.
 * <p>
 * A timetable row links a course, a room, and a time slot; it is referenced when
 * opening an attendance session on a scheduled class.
 */
@RestController
@RequestMapping("/timetables")
public class TimetableController {

    private final TimetableService timetableService;

    public TimetableController(TimetableService timetableService) {
        this.timetableService = timetableService;
    }

    /**
     * Lists every timetable entry.
     *
     * @return all entries
     */
    @GetMapping
    public List<Timetable> findAll() {
        return timetableService.findAll();
    }

    /**
     * Returns a single timetable entry by ID.
     *
     * @param id timetable ID
     * @return the entry, or {@code 404 Not Found}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Timetable> findById(@PathVariable Long id) {
        return timetableService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a new timetable entry.
     *
     * @param timetable timetable payload
     * @return {@code 201 Created}
     */
    @PostMapping
    public ResponseEntity<Void> save(@RequestBody Timetable timetable) {
        timetableService.save(timetable);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Updates an existing timetable entry.
     *
     * @param id        timetable ID
     * @param timetable updated fields
     * @return {@code 200 OK}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody Timetable timetable) {
        timetable.setId(id);
        timetableService.update(timetable);
        return ResponseEntity.ok().build();
    }

    /**
     * Deletes a timetable entry.
     *
     * @param id timetable ID
     * @return {@code 204 No Content}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        timetableService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
