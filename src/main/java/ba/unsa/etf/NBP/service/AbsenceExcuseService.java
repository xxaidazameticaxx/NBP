package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.model.*;
import ba.unsa.etf.NBP.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AbsenceExcuseService {

    private final AbsenceExcuseRepository absenceExcuseRepository;
    private final AttendanceRepository attendanceRepository;
    private final CourseSessionRepository courseSessionRepository;
    private final CourseRepository courseRepository;
    private final ProfessorRepository professorRepository;
    private final StudentRepository studentRepository;
    private final NotificationService notificationService;
    private final DataSource dataSource;

    public AbsenceExcuseService(AbsenceExcuseRepository absenceExcuseRepository,
                                AttendanceRepository attendanceRepository,
                                CourseSessionRepository courseSessionRepository,
                                CourseRepository courseRepository,
                                ProfessorRepository professorRepository,
                                StudentRepository studentRepository,
                                NotificationService notificationService,
                                DataSource dataSource) {
        this.absenceExcuseRepository = absenceExcuseRepository;
        this.attendanceRepository = attendanceRepository;
        this.courseSessionRepository = courseSessionRepository;
        this.courseRepository = courseRepository;
        this.professorRepository = professorRepository;
        this.studentRepository = studentRepository;
        this.notificationService = notificationService;
        this.dataSource = dataSource;
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

    /**
     * Submits a new absence excuse via EXCUSE_PKG.SUBMIT_EXCUSE.
     */
    public AbsenceExcuse submitExcuse(Long courseSessionId, String reason, byte[] document, String documentName, User currentUser) {
        Student student = studentRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a student"));

        try (Connection conn = dataSource.getConnection()) {
            CallableStatement cs = conn.prepareCall(
                    "{call NBPT3.EXCUSE_PKG.SUBMIT_EXCUSE(?, ?, ?, ?, ?)}"
            );
            cs.setLong(1, student.getId());
            cs.setLong(2, courseSessionId);
            cs.setString(3, reason);
            cs.setString(4, documentName);
            cs.registerOutParameter(5, Types.NUMERIC);
            cs.execute();

            Long excuseId = cs.getLong(5);

            AbsenceExcuse excuse = absenceExcuseRepository.findById(excuseId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Excuse not found after insert"));

            // Azuriraj BLOB dokument ako postoji
            if (document != null) {
                excuse.setDocument(document);
                absenceExcuseRepository.update(excuse);
            }

            return excuse;

        } catch (SQLException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Approves a pending excuse via EXCUSE_PKG.APPROVE_EXCUSE.
     */
    public void approveExcuse(Long excuseId, User currentUser) {
        Professor professor = professorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a professor"));

        try (Connection conn = dataSource.getConnection()) {
            CallableStatement cs = conn.prepareCall(
                    "{call NBPT3.EXCUSE_PKG.APPROVE_EXCUSE(?, ?)}"
            );
            cs.setLong(1, excuseId);
            cs.setLong(2, professor.getId());
            cs.execute();

        } catch (SQLException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Rejects a pending excuse via EXCUSE_PKG.REJECT_EXCUSE.
     */
    public void rejectExcuse(Long excuseId, User currentUser) {
        Professor professor = professorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a professor"));

        try (Connection conn = dataSource.getConnection()) {
            CallableStatement cs = conn.prepareCall(
                    "{call NBPT3.EXCUSE_PKG.REJECT_EXCUSE(?, ?)}"
            );
            cs.setLong(1, excuseId);
            cs.setLong(2, professor.getId());
            cs.execute();

        } catch (SQLException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    public List<AbsenceExcuse> findByStudentId(Long studentId) {
        return absenceExcuseRepository.findByStudentId(studentId);
    }

    public List<AbsenceExcuse> findByCourseSessionId(Long courseSessionId) {
        return absenceExcuseRepository.findByCourseSessionId(courseSessionId);
    }

    public List<AbsenceExcuse> findPendingByProfessor(User currentUser) {
        Professor professor = professorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a professor"));
        return absenceExcuseRepository.findPendingByProfessorId(professor.getId());
    }
}