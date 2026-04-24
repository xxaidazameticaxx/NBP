package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.model.Professor;
import ba.unsa.etf.NBP.repository.ProfessorRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service layer for professor records.
 * <p>
 * {@link #findByUserId(Long)} bridges the logged-in user to the matching
 * professor record, which is required for authorization checks on course ownership.
 */
@Service
public class ProfessorService {

    private final ProfessorRepository professorRepository;

    public ProfessorService(ProfessorRepository professorRepository) {
        this.professorRepository = professorRepository;
    }

    /**
     * Returns every professor.
     *
     * @return all professors
     */
    public List<Professor> findAll() {
        return professorRepository.findAll();
    }

    /**
     * Looks up a professor by ID.
     *
     * @param id professor ID
     * @return the professor, or {@link Optional#empty()} if missing
     */
    public Optional<Professor> findById(Long id) {
        return professorRepository.findById(id);
    }

    /**
     * Finds the professor record whose {@code USER_ID} matches the given user.
     *
     * @param userId the user's ID
     * @return the professor, or {@link Optional#empty()} if none
     */
    public Optional<Professor> findByUserId(Long userId) {
        return professorRepository.findByUserId(userId);
    }

    /**
     * Inserts a new professor row.
     *
     * @param professor professor to insert
     */
    public void save(Professor professor) {
        professorRepository.save(professor);
    }

    /**
     * Updates a professor row.
     *
     * @param professor professor with updated fields (ID required)
     */
    public void update(Professor professor) {
        professorRepository.update(professor);
    }

    /**
     * Deletes a professor by ID.
     *
     * @param id professor ID
     */
    public void deleteById(Long id) {
        professorRepository.deleteById(id);
    }

}
