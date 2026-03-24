package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.model.Attendance;
import ba.unsa.etf.NBP.repository.AttendanceRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;

    public AttendanceService(AttendanceRepository attendanceRepository) {
        this.attendanceRepository = attendanceRepository;
    }

    public List<Attendance> findAll() {
        return attendanceRepository.findAll();
    }

    public Optional<Attendance> findById(Long id) {
        return attendanceRepository.findById(id);
    }

    public void save(Attendance attendance) {
        attendanceRepository.save(attendance);
    }

    public void update(Attendance attendance) {
        attendanceRepository.update(attendance);
    }

    public void deleteById(Long id) {
        attendanceRepository.deleteById(id);
    }

    public List<Attendance> findByStudentId(Long studentId) {
        return attendanceRepository.findByStudentId(studentId);
    }

    public List<Attendance> findByCourseSessionId(Long courseSessionId) {
        return attendanceRepository.findByCourseSessionId(courseSessionId);
    }
}
