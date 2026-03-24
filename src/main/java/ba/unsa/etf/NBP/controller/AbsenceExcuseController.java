package ba.unsa.etf.NBP.controller;

import ba.unsa.etf.NBP.model.AbsenceExcuse;
import ba.unsa.etf.NBP.service.AbsenceExcuseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/absenceExcuses")
public class AbsenceExcuseController {

    private final AbsenceExcuseService absenceExcuseService;

    public AbsenceExcuseController(AbsenceExcuseService absenceExcuseService) {
        this.absenceExcuseService = absenceExcuseService;
    }

    @GetMapping
    public List<AbsenceExcuse> findAll() {
        return absenceExcuseService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<AbsenceExcuse> findById(@PathVariable Long id) {
        return absenceExcuseService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Void> save(@RequestBody AbsenceExcuse absenceExcuse) {
        absenceExcuseService.save(absenceExcuse);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody AbsenceExcuse absenceExcuse) {
        absenceExcuse.setId(id);
        absenceExcuseService.update(absenceExcuse);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        absenceExcuseService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
