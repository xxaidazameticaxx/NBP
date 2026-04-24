package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.model.StudyProgram;
import ba.unsa.etf.NBP.repository.StudyProgramRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Thin service layer over {@link StudyProgramRepository} providing CRUD for study programs.
 */
@Service
public class StudyProgramService {

    private final StudyProgramRepository studyProgramRepository;

    public StudyProgramService(StudyProgramRepository studyProgramRepository) {
        this.studyProgramRepository = studyProgramRepository;
    }

    /**
     * Returns every study program.
     *
     * @return all study programs
     */
    public List<StudyProgram> findAll() {
        return studyProgramRepository.findAll();
    }

    /**
     * Looks up a study program by ID.
     *
     * @param id study program ID
     * @return the study program, or {@link Optional#empty()} if missing
     */
    public Optional<StudyProgram> findById(Long id) {
        return studyProgramRepository.findById(id);
    }

    /**
     * Inserts a new study program row.
     *
     * @param studyProgram study program to insert
     */
    public void save(StudyProgram studyProgram) {
        studyProgramRepository.save(studyProgram);
    }

    /**
     * Updates a study program row.
     *
     * @param studyProgram study program with updated fields (ID required)
     */
    public void update(StudyProgram studyProgram) {
        studyProgramRepository.update(studyProgram);
    }

    /**
     * Deletes a study program by ID.
     *
     * @param id study program ID
     */
    public void deleteById(Long id) {
        studyProgramRepository.deleteById(id);
    }
}
