package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.dto.attendance.SessionAttendanceRecordResponse;
import ba.unsa.etf.NBP.model.Attendance;
import ba.unsa.etf.NBP.model.Course;
import ba.unsa.etf.NBP.model.CourseSession;
import ba.unsa.etf.NBP.model.Professor;
import ba.unsa.etf.NBP.model.Student;
import ba.unsa.etf.NBP.model.User;
import ba.unsa.etf.NBP.repository.AttendanceRepository;
import ba.unsa.etf.NBP.repository.CourseRepository;
import ba.unsa.etf.NBP.repository.CourseSessionRepository;
import ba.unsa.etf.NBP.repository.EnrollmentRepository;
import ba.unsa.etf.NBP.repository.ProfessorRepository;
import ba.unsa.etf.NBP.repository.StudentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final CourseSessionRepository courseSessionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final ProfessorRepository professorRepository;
    private final CourseRepository courseRepository;

    public AttendanceService(AttendanceRepository attendanceRepository,
                             CourseSessionRepository courseSessionRepository,
                             EnrollmentRepository enrollmentRepository,
                             StudentRepository studentRepository,
                             ProfessorRepository professorRepository,
                             CourseRepository courseRepository) {
        this.attendanceRepository = attendanceRepository;
        this.courseSessionRepository = courseSessionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.professorRepository = professorRepository;
        this.courseRepository = courseRepository;
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

    public Attendance registerAttendance(String sessionCode, User currentUser) {
        if (sessionCode == null || sessionCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sessionCode is required");
        }

        CourseSession session = courseSessionRepository.findBySessionCode(sessionCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        if (session.getSessionEndTime() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session is closed");
        }

        Student student = studentRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a student"));

        if (!enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), session.getCourseId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student is not enrolled in this course");
        }

        if (attendanceRepository.existsByStudentIdAndCourseSessionId(student.getId(), session.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attendance already registered for this session");
        }

        Attendance attendance = new Attendance();
        attendance.setStudentId(student.getId());
        attendance.setCourseSessionId(session.getId());
        attendance.setPresent(true);
        attendance.setMarkedAt(LocalDateTime.now());

        Long id = attendanceRepository.saveAndReturnId(attendance);
        attendance.setId(id);
        return attendance;
    }

    public int autoMarkAbsentForSession(CourseSession session) {
        if (session == null || session.getId() == null || session.getCourseId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid session");
        }
        if (session.getSessionEndTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session is not closed");
        }
        return attendanceRepository.autoInsertAbsentForMissingEnrolledStudents(session.getCourseId(), session.getId());
    }

    public Attendance overrideAttendancePresence(Long attendanceId, boolean isPresent, User currentUser) {
        Professor professor = professorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a professor"));

        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendance not found"));

        CourseSession session = courseSessionRepository.findById(attendance.getCourseSessionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        Course course = courseRepository.findById(session.getCourseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (!course.getProfessorId().equals(professor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this session");
        }

        attendanceRepository.updateIsPresent(attendanceId, isPresent);
        attendance.setPresent(isPresent);
        return attendance;
    }

    public List<SessionAttendanceRecordResponse> getAttendanceForSession(Long courseSessionId, User currentUser) {
        assertProfessorOwnsSession(courseSessionId, currentUser);
        return attendanceRepository.findSessionAttendanceWithStudentNames(courseSessionId);
    }

    public List<Attendance> getAttendanceHistoryForStudent(Long studentId, User currentUser) {
        Long roleId = currentUser.getRole() != null ? currentUser.getRole().getId() : null;
        if (roleId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User has no role");
        }

        if (roleId == 1L) {
            Student currentStudent = studentRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a student"));
            if (!currentStudent.getId().equals(studentId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Students can only view their own attendance history");
            }
        }

        return attendanceRepository.findByStudentId(studentId);
    }

    private void assertProfessorOwnsSession(Long courseSessionId, User currentUser) {
        Professor professor = professorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a professor"));

        CourseSession session = courseSessionRepository.findById(courseSessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        Course course = courseRepository.findById(session.getCourseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (!course.getProfessorId().equals(professor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this session");
        }
    }
}
