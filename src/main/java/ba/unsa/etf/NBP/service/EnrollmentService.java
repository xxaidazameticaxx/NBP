package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.dto.enrollment.StudentCourseDto;
import ba.unsa.etf.NBP.model.Enrollment;
import ba.unsa.etf.NBP.repository.AttendanceRepository;
import ba.unsa.etf.NBP.repository.EnrollmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;

    public EnrollmentService(EnrollmentRepository enrollmentRepository, AttendanceRepository attendanceRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.attendanceRepository = attendanceRepository;
    }

    public List<Enrollment> findAll() {
        return enrollmentRepository.findAll();
    }

    public Optional<Enrollment> findById(Long id) {
        return enrollmentRepository.findById(id);
    }

    public void save(Enrollment enrollment) {
        enrollmentRepository.save(enrollment);
    }

    public void update(Enrollment enrollment) {
        enrollmentRepository.update(enrollment);
    }

    @Transactional
    public void deleteById(Long id) {
        Optional<Enrollment> enrollmentOpt = enrollmentRepository.findById(id);
        if (enrollmentOpt.isPresent()) {
            Enrollment enrollment = enrollmentOpt.get();
            attendanceRepository.deleteByStudentIdAndCourseId(enrollment.getStudentId(), enrollment.getCourseId());
            enrollmentRepository.deleteById(id);
        }
    }

    public List<Enrollment> findByStudentId(Long studentId) {
        return enrollmentRepository.findByStudentId(studentId);
    }

    public List<Enrollment> findByCourseId(Long courseId) {
        return enrollmentRepository.findByCourseId(courseId);
    }

    public boolean existsByStudentAndCourse(Long studentId, Long courseId) {
        return enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId);
    }

    public List<StudentCourseDto> findCoursesByStudentIdWithDetails(Long studentId) {
        return enrollmentRepository.findCoursesByStudentIdWithDetails(studentId);
    }
}