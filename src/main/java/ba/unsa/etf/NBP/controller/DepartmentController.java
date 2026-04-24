package ba.unsa.etf.NBP.controller;

import ba.unsa.etf.NBP.model.Department;
import ba.unsa.etf.NBP.service.DepartmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD endpoints for university departments under {@code /departments}.
 */
@RestController
@RequestMapping("/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    /**
     * Lists every department.
     *
     * @return all departments
     */
    @GetMapping
    public List<Department> findAll() {
        return departmentService.findAll();
    }

    /**
     * Returns a single department by ID.
     *
     * @param id department ID
     * @return the department, or {@code 404 Not Found}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Department> findById(@PathVariable Long id) {
        return departmentService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a new department.
     *
     * @param department department payload
     * @return {@code 201 Created}
     */
    @PostMapping
    public ResponseEntity<Void> save(@RequestBody Department department) {
        departmentService.save(department);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Updates an existing department.
     *
     * @param id         department ID
     * @param department updated fields
     * @return {@code 200 OK}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody Department department) {
        department.setId(id);
        departmentService.update(department);
        return ResponseEntity.ok().build();
    }

    /**
     * Deletes a department.
     *
     * @param id department ID
     * @return {@code 204 No Content}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        departmentService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
