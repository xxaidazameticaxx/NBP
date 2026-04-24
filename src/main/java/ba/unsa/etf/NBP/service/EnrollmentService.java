package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.dto.enrollment.StudentCourseDto;
import ba.unsa.etf.NBP.model.Enrollment;
import ba.unsa.etf.NBP.repository.AttendanceRepository;
import ba.unsa.etf.NBP.repository.EnrollmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * CRUD and lookups for student course enrollments.
 * <p>
 * Deletes cascade to attendance records to maintain referential integrity.
 */
@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;

    public EnrollmentService(EnrollmentRepository enrollmentRepository, AttendanceRepository attendanceRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.attendanceRepository = attendanceRepository;
    }

    /**
     * Returns every enrollment.
     *
     * @return all enrollments
     */
    public List<Enrollment> findAll() {
        return enrollmentRepository.findAll();
    }

    /**
     * Looks up an enrollment by ID.
     *
     * @param id enrollment ID
     * @return the enrollment, or {@link Optional#empty()} if missing
     */
    public Optional<Enrollment> findById(Long id) {
        return enrollmentRepository.findById(id);
    }

    /**
     * Inserts a new enrollment.
     *
     * @param enrollment enrollment to insert
     */
    public void save(Enrollment enrollment) {
        enrollmentRepository.save(enrollment);
    }

    /**
     * Updates an enrollment.
     *
     * @param enrollment enrollment with updated fields (ID required)
     */
    public void update(Enrollment enrollment) {
        enrollmentRepository.update(enrollment);
    }

    /**
     * Deletes an enrollment and its associated attendance records.
     *
     * @param id enrollment ID
     */
    @Transactional
    public void deleteById(Long id) {
        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findById(id);
        if (enrollmentOpt.isPresent()) {
            Enrollment enrollment = enrollmentOpt.get();
            attendanceRepository.deleteByStudentIdAndCourseId(enrollment.getStudentId(), enrollment.getCourseId());
            enrollmentRepository.deleteById(id);
        }
    }

    /**
     * Returns all enrollments for a student.
     *
     * @param studentId student ID
     * @return enrollments for that student
     */
    public List<Enrollment> findByStudentId(Long studentId) {
        return enrollmentRepository.findByStudentId(studentId);
    }

    /**
     * Returns all enrollments for a course.
     *
     * @param courseId course ID
     * @return enrollments in that course
     */
    public List<Enrollment> findByCourseId(Long courseId) {
        return enrollmentRepository.findByCourseId(courseId);
    }

    /**
     * Checks if a student is enrolled in a course.
     *
     * @param studentId student ID
     * @param courseId course ID
     * @return true if enrolled, false otherwise
     */
    public boolean existsByStudentAndCourse(Long studentId, Long courseId) {
        return enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId);
    }

    /**
     * Returns a student's enrolled courses with full details.
     *
     * @param studentId student ID
     * @return list of courses with details
     */
    public List<StudentCourseDto> findCoursesByStudentIdWithDetails(Long studentId) {
        return enrollmentRepository.findCoursesByStudentIdWithDetails(studentId);
    }
}