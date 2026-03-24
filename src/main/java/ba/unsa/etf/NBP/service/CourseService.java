package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.model.Course;
import ba.unsa.etf.NBP.repository.CourseRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class CourseService {

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    public List<Course> findAll() {
        return courseRepository.findAll();
    }

    public Optional<Course> findById(Long id) {
        return courseRepository.findById(id);
    }

    public void save(Course course) {
        courseRepository.save(course);
    }

    public void update(Course course) {
        courseRepository.update(course);
    }

    public void deleteById(Long id) {
        courseRepository.deleteById(id);
    }

    public List<Course> findByProfessorId(Long professorId) {
        return courseRepository.findByProfessorId(professorId);
    }

    public List<Course> findByDepartmentId(Long departmentId) {
        return courseRepository.findByDepartmentId(departmentId);
    }
}
