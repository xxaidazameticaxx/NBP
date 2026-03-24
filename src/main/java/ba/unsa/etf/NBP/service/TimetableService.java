package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.model.Timetable;
import ba.unsa.etf.NBP.repository.TimetableRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TimetableService {

    private final TimetableRepository timetableRepository;

    public TimetableService(TimetableRepository timetableRepository) {
        this.timetableRepository = timetableRepository;
    }

    public List<Timetable> findAll() {
        return timetableRepository.findAll();
    }

    public Optional<Timetable> findById(Long id) {
        return timetableRepository.findById(id);
    }

    public void save(Timetable timetable) {
        timetableRepository.save(timetable);
    }

    public void update(Timetable timetable) {
        timetableRepository.update(timetable);
    }

    public void deleteById(Long id) {
        timetableRepository.deleteById(id);
    }
}