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

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Attendance registration, manual overrides, and auto-absence bulk insert.
 * <p>
 * Students register their presence by submitting the professor's 6-digit
 * session code. When a session is closed, {@link #autoMarkAbsentForSession(CourseSession)}
 * inserts an "absent" record for every enrolled student who did not check in.
 */
@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final CourseSessionRepository courseSessionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final ProfessorRepository professorRepository;
    private final CourseRepository courseRepository;
    private final DataSource dataSource;
    private final AttendanceAlertService alertService;

    public AttendanceService(AttendanceRepository attendanceRepository,
                             CourseSessionRepository courseSessionRepository,
                             EnrollmentRepository enrollmentRepository,
                             StudentRepository studentRepository,
                             ProfessorRepository professorRepository,
                             CourseRepository courseRepository,
                             DataSource dataSource) {
                             AttendanceAlertService alertService) {
        this.attendanceRepository = attendanceRepository;
        this.courseSessionRepository = courseSessionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.professorRepository = professorRepository;
        this.courseRepository = courseRepository;
        this.dataSource = dataSource;
        this.alertService = alertService;
    }

    /**
     * Returns every attendance record.
     *
     * @return all attendance rows
     */
    public List<Attendance> findAll() {
        return attendanceRepository.findAll();
    }

    /**
     * Looks up an attendance row by ID.
     *
     * @param id attendance ID
     * @return the row, or {@link Optional#empty()} if missing
     */
    public Optional<Attendance> findById(Long id) {
        return attendanceRepository.findById(id);
    }

    /**
     * Inserts an attendance row directly (low level).
     *
     * @param attendance attendance row to insert
     */
    public void save(Attendance attendance) {
        attendanceRepository.save(attendance);
    }

    /**
     * Updates an attendance row.
     *
     * @param attendance row with updated fields (ID required)
     */
    public void update(Attendance attendance) {
        attendanceRepository.update(attendance);
    }

    /**
     * Deletes an attendance row by ID.
     *
     * @param id attendance ID
     */
    public void deleteById(Long id) {
        attendanceRepository.deleteById(id);
    }

    /**
     * Returns every attendance row for the given student.
     *
     * @param studentId student ID
     * @return attendance history for that student
     */
    public List<Attendance> findByStudentId(Long studentId) {
        return attendanceRepository.findByStudentId(studentId);
    }

    /**
     * Registers the caller as present using the session code the professor shared.
     * <p>
     * Rejects if the session is closed, the user is not a student, the student is
     * not enrolled in the course, or attendance was already registered for the session.
     *
     * @param sessionCode the 6-digit code
     * @param currentUser the authenticated student
     * @return the newly created attendance row with its ID
     * @throws ResponseStatusException on any validation failure
     */
    public Attendance registerAttendance(String sessionCode, User currentUser) {
        if (sessionCode == null || sessionCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sessionCode is required");
        }

        Student student = studentRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a student"));

        try (Connection conn = dataSource.getConnection()) {
            CallableStatement cs = conn.prepareCall(
                    "{call NBPT3.ATTENDANCE_PKG.REGISTER_ATTENDANCE(?, ?)}"
            );
            cs.setString(1, sessionCode);
            cs.setLong(2, student.getId());
            cs.execute();

        } catch (SQLException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        // Dohvati kreirani attendance iz baze da vratimo response
        CourseSession session = courseSessionRepository.findBySessionCode(sessionCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        return attendanceRepository
                .findByStudentIdAndCourseSessionId(student.getId(), session.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Attendance not found after insert"));
        Long id = attendanceRepository.saveAndReturnId(attendance);
        attendance.setId(id);

        // Trigger real-time alert analysis
        alertService.analyzeAndCreateAlert(student.getId(), session.getCourseId());

        return attendance;
    }

    /**
     * Inserts an "absent" row for every enrolled student who did not register
     * attendance for the given closed session.
     * <p>
     * Invoked during session close to finalize the roster.
     *
     * @param session the closed session
     * @return number of absent rows inserted
     * @throws ResponseStatusException 400 if the session is invalid or still open
     */
    public int autoMarkAbsentForSession(CourseSession session) {
        if (session == null || session.getId() == null || session.getCourseId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid session");
        }
        if (session.getSessionEndTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session is not closed");
        }
        int absentsMarked = attendanceRepository.autoInsertAbsentForMissingEnrolledStudents(session.getCourseId(), session.getId());

        // Trigger real-time alert analysis for all students in this course
        enrollmentRepository.findByCourseId(session.getCourseId())
                .forEach(enrollment -> alertService.analyzeAndCreateAlert(enrollment.getStudentId(), session.getCourseId()));

        return absentsMarked;
    }

    /**
     * Changes the presence flag on an attendance row. Only the session's owning professor may call this.
     *
     * @param attendanceId attendance row ID
     * @param isPresent    new value for the present flag
     * @param currentUser  the authenticated professor
     * @return the updated attendance row
     * @throws ResponseStatusException 403 if caller is not the owning professor,
     *         404 if the attendance, session, or course is missing
     */
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

        // Trigger real-time alert analysis after override
        Student student = studentRepository.findById(attendance.getStudentId())
                .orElse(null);
        if (student != null) {
            alertService.analyzeAndCreateAlert(student.getId(), session.getCourseId());
        }

        return attendance;
    }

    /**
     * Returns attendance rows for one session, with student names attached,
     * for display in the professor's dashboard.
     *
     * @param courseSessionId session ID
     * @param currentUser     the authenticated professor
     * @return attendance records with student info
     */
    public List<SessionAttendanceRecordResponse> getAttendanceForSession(Long courseSessionId, User currentUser) {
        assertProfessorOwnsSession(courseSessionId, currentUser);
        return attendanceRepository.findSessionAttendanceWithStudentNames(courseSessionId);
    }

    /**
     * Returns the full attendance history for a student.
     * <p>
     * Students may only view their own history; all other roles are allowed.
     *
     * @param studentId   target student ID
     * @param currentUser the authenticated user
     * @return attendance records
     * @throws ResponseStatusException 403 if a student tries to view someone else's history
     */
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

    /**
     * Throws 403 unless the calling user is the owning professor of the given session's course.
     *
     * @param courseSessionId session ID
     * @param currentUser     the authenticated user
     * @throws ResponseStatusException on any ownership or lookup failure
     */
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
