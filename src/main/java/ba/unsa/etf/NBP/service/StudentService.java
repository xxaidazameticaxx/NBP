package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.dto.enrollment.EnrolledStudentDto;
import ba.unsa.etf.NBP.model.Student;
import ba.unsa.etf.NBP.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * CRUD and lookups for student records.
 * <p>
 * {@link #findByUserId(Long)} bridges the logged-in user to the matching
 * student record, which is required for authorization checks on attendance and enrollments.
 */
@Service
public class StudentService {

    private final StudentRepository studentRepository;

    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    /**
     * Returns every student.
     *
     * @return all students
     */
    public List<Student> findAll() {
        return studentRepository.findAll();
    }

    /**
     * Looks up a student by ID.
     *
     * @param id student ID
     * @return the student, or {@link Optional#empty()} if missing
     */
    public Optional<Student> findById(Long id) {
        return studentRepository.findById(id);
    }

    /**
     * Finds the student record whose {@code USER_ID} matches the given user.
     *
     * @param userId the user's ID
     * @return the student, or {@link Optional#empty()} if none
     */
    public Optional<Student> findByUserId(Long userId) {
        return studentRepository.findByUserId(userId);
    }

    /**
     * Inserts a new student row.
     *
     * @param student student to insert
     */
    public void save(Student student) {
        studentRepository.save(student);
    }

    /**
     * Updates a student row.
     *
     * @param student student with updated fields (ID required)
     */
    public void update(Student student) {
        studentRepository.update(student);
    }

    /**
     * Deletes a student by ID.
     *
     * @param id student ID
     */
    public void deleteById(Long id) {
        studentRepository.deleteById(id);
    }

    /**
     * Returns the roster of students enrolled in a course.
     *
     * @param courseId course ID
     * @return enrolled students for that course
     */
    public List<EnrolledStudentDto> findRosterByCourseId(Long courseId) {
        return studentRepository.findRosterByCourseId(courseId);
    }
}