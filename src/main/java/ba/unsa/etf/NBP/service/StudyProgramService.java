package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.model.StudyProgram;
import ba.unsa.etf.NBP.repository.StudyProgramRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StudyProgramService {

    private final StudyProgramRepository studyProgramRepository;

    public StudyProgramService(StudyProgramRepository studyProgramRepository) {
        this.studyProgramRepository = studyProgramRepository;
    }

    public List<StudyProgram> findAll() {
        return studyProgramRepository.findAll();
    }

    public Optional<StudyProgram> findById(Long id) {
        return studyProgramRepository.findById(id);
    }

    public void save(StudyProgram studyProgram) {
        studyProgramRepository.save(studyProgram);
    }

    public void update(StudyProgram studyProgram) {
        studyProgramRepository.update(studyProgram);
    }

    public void deleteById(Long id) {
        studyProgramRepository.deleteById(id);
    }
}