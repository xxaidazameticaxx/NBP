package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.model.Timetable;
import ba.unsa.etf.NBP.repository.TimetableRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Thin service layer over {@link TimetableRepository} providing CRUD for
 * scheduled class entries (timetables).
 */
@Service
public class TimetableService {

    private final TimetableRepository timetableRepository;

    public TimetableService(TimetableRepository timetableRepository) {
        this.timetableRepository = timetableRepository;
    }

    /**
     * Returns every timetable entry.
     *
     * @return all entries
     */
    public List<Timetable> findAll() {
        return timetableRepository.findAll();
    }

    /**
     * Looks up a timetable entry by ID.
     *
     * @param id timetable ID
     * @return the entry, or {@link Optional#empty()} if missing
     */
    public Optional<Timetable> findById(Long id) {
        return timetableRepository.findById(id);
    }

    /**
     * Inserts a new timetable entry.
     *
     * @param timetable entry to insert
     */
    public void save(Timetable timetable) {
        timetableRepository.save(timetable);
    }

    /**
     * Updates a timetable entry.
     *
     * @param timetable entry with updated fields (ID required)
     */
    public void update(Timetable timetable) {
        timetableRepository.update(timetable);
    }

    /**
     * Deletes a timetable entry by ID.
     *
     * @param id timetable ID
     */
    public void deleteById(Long id) {
        timetableRepository.deleteById(id);
    }
}
