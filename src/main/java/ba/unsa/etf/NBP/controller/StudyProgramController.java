package ba.unsa.etf.NBP.controller;

import ba.unsa.etf.NBP.model.StudyProgram;
import ba.unsa.etf.NBP.service.StudyProgramService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/study-programs")
public class StudyProgramController {

    private final StudyProgramService studyProgramService;

    public StudyProgramController(StudyProgramService studyProgramService) {
        this.studyProgramService = studyProgramService;
    }

    @GetMapping
    public List<StudyProgram> findAll() {
        return studyProgramService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudyProgram> findById(@PathVariable Long id) {
        return studyProgramService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Void> save(@RequestBody StudyProgram studyProgram) {
        studyProgramService.save(studyProgram);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody StudyProgram studyProgram) {
        studyProgram.setId(id);
        studyProgramService.update(studyProgram);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        studyProgramService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}