package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.model.Professor;
import ba.unsa.etf.NBP.repository.ProfessorRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProfessorService {

    private final ProfessorRepository professorRepository;

    public ProfessorService(ProfessorRepository professorRepository) {
        this.professorRepository = professorRepository;
    }

    public List<Professor> findAll() {
        return professorRepository.findAll();
    }

    public Optional<Professor> findById(Long id) {
        return professorRepository.findById(id);
    }

    public void save(Professor professor) {
        professorRepository.save(professor);
    }

    public void update(Professor professor) {
        professorRepository.update(professor);
    }

    public void deleteById(Long id) {
        professorRepository.deleteById(id);
    }

}