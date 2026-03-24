package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.model.Enrollment;
import ba.unsa.etf.NBP.repository.EnrollmentRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;

    public EnrollmentService(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
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

    public void deleteById(Long id) {
        enrollmentRepository.deleteById(id);
    }

    public List<Enrollment> findByStudentId(Long studentId) {
        return enrollmentRepository.findByStudentId(studentId);
    }

    public List<Enrollment> findByCourseId(Long courseId) {
        return enrollmentRepository.findByCourseId(courseId);
    }
}
