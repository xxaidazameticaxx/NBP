package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.model.CourseSession;
import ba.unsa.etf.NBP.repository.CourseSessionRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class CourseSessionService {

    private final CourseSessionRepository courseSessionRepository;

    public CourseSessionService(CourseSessionRepository courseSessionRepository) {
        this.courseSessionRepository = courseSessionRepository;
    }

    public List<CourseSession> findAll() {
        return courseSessionRepository.findAll();
    }

    public Optional<CourseSession> findById(Long id) {
        return courseSessionRepository.findById(id);
    }

    public void save(CourseSession courseSession) {
        courseSessionRepository.save(courseSession);
    }

    public void update(CourseSession courseSession) {
        courseSessionRepository.update(courseSession);
    }

    public void deleteById(Long id) {
        courseSessionRepository.deleteById(id);
    }
}
