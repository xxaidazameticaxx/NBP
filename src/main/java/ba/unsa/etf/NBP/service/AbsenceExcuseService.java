package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.model.AbsenceExcuse;
import ba.unsa.etf.NBP.repository.AbsenceExcuseRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class AbsenceExcuseService {

    private final AbsenceExcuseRepository absenceExcuseRepository;

    public AbsenceExcuseService(AbsenceExcuseRepository absenceExcuseRepository) {
        this.absenceExcuseRepository = absenceExcuseRepository;
    }

    public List<AbsenceExcuse> findAll() {
        return absenceExcuseRepository.findAll();
    }

    public Optional<AbsenceExcuse> findById(Long id) {
        return absenceExcuseRepository.findById(id);
    }

    public void save(AbsenceExcuse absenceExcuse) {
        absenceExcuseRepository.save(absenceExcuse);
    }

    public void update(AbsenceExcuse absenceExcuse) {
        absenceExcuseRepository.update(absenceExcuse);
    }

    public void deleteById(Long id) {
        absenceExcuseRepository.deleteById(id);
    }
}
