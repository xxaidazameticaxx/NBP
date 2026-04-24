package ba.unsa.etf.NBP.controller;

import ba.unsa.etf.NBP.model.StudyProgram;
import ba.unsa.etf.NBP.service.StudyProgramService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD endpoints for study programs under {@code /study-programs}.
 */
@RestController
@RequestMapping("/study-programs")
public class StudyProgramController {

    private final StudyProgramService studyProgramService;

    public StudyProgramController(StudyProgramService studyProgramService) {
        this.studyProgramService = studyProgramService;
    }

    /**
     * Lists every study program.
     *
     * @return all study programs
     */
    @GetMapping
    public List<StudyProgram> findAll() {
        return studyProgramService.findAll();
    }

    /**
     * Returns a single study program by ID.
     *
     * @param id study program ID
     * @return the study program, or {@code 404 Not Found}
     */
    @GetMapping("/{id}")
    public ResponseEntity<StudyProgram> findById(@PathVariable Long id) {
        return studyProgramService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a new study program.
     *
     * @param studyProgram study program payload
     * @return {@code 201 Created}
     */
    @PostMapping
    public ResponseEntity<Void> save(@RequestBody StudyProgram studyProgram) {
        studyProgramService.save(studyProgram);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Updates an existing study program.
     *
     * @param id           study program ID
     * @param studyProgram updated fields
     * @return {@code 200 OK}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody StudyProgram studyProgram) {
        studyProgram.setId(id);
        studyProgramService.update(studyProgram);
        return ResponseEntity.ok().build();
    }

    /**
     * Deletes a study program.
     *
     * @param id study program ID
     * @return {@code 204 No Content}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        studyProgramService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
