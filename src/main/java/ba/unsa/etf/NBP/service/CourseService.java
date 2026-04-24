package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.model.Course;
import ba.unsa.etf.NBP.repository.CourseRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for courses.
 * <p>
 * Provides CRUD plus lookups by professor and by department.
 */
@Service
public class CourseService {

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    /**
     * Returns every course.
     *
     * @return all courses
     */
    public List<Course> findAll() {
        return courseRepository.findAll();
    }

    /**
     * Looks up a course by ID.
     *
     * @param id course ID
     * @return the course, or {@link Optional#empty()} if missing
     */
    public Optional<Course> findById(Long id) {
        return courseRepository.findById(id);
    }

    /**
     * Inserts a new course.
     *
     * @param course course to insert
     */
    public void save(Course course) {
        courseRepository.save(course);
    }

    /**
     * Updates a course row.
     *
     * @param course course with updated fields (ID required)
     */
    public void update(Course course) {
        courseRepository.update(course);
    }

    /**
     * Deletes a course by ID.
     *
     * @param id course ID
     */
    public void deleteById(Long id) {
        courseRepository.deleteById(id);
    }

    /**
     * Returns every course taught by the given professor.
     *
     * @param professorId professor ID
     * @return courses owned by that professor
     */
    public List<Course> findByProfessorId(Long professorId) {
        return courseRepository.findByProfessorId(professorId);
    }

    /**
     * Returns every course offered by the given department.
     *
     * @param departmentId department ID
     * @return courses in that department
     */
    public List<Course> findByDepartmentId(Long departmentId) {
        return courseRepository.findByDepartmentId(departmentId);
    }
}
