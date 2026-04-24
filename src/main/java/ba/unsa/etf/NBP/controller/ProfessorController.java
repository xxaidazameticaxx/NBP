package ba.unsa.etf.NBP.controller;

import ba.unsa.etf.NBP.model.Professor;
import ba.unsa.etf.NBP.service.ProfessorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD endpoints for professor records under {@code /professors}.
 */
@RestController
@RequestMapping("/professors")
public class ProfessorController {

    private final ProfessorService professorService;

    public ProfessorController(ProfessorService professorService) {
        this.professorService = professorService;
    }

    /**
     * Lists every professor.
     *
     * @return all professors
     */
    @GetMapping
    public List<Professor> findAll() {
        return professorService.findAll();
    }

    /**
     * Returns a single professor by ID.
     *
     * @param id professor ID
     * @return the professor, or {@code 404 Not Found}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Professor> findById(@PathVariable Long id) {
        return professorService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a new professor.
     *
     * @param professor professor payload
     * @return {@code 201 Created}
     */
    @PostMapping
    public ResponseEntity<Void> save(@RequestBody Professor professor) {
        professorService.save(professor);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Updates an existing professor.
     *
     * @param id        professor ID
     * @param professor updated fields
     * @return {@code 200 OK}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody Professor professor) {
        professor.setId(id);
        professorService.update(professor);
        return ResponseEntity.ok().build();
    }

    /**
     * Deletes a professor.
     *
     * @param id professor ID
     * @return {@code 204 No Content}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        professorService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
