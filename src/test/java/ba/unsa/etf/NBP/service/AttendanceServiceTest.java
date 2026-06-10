package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.dto.attendance.SessionAttendanceRecordResponse;
import ba.unsa.etf.NBP.model.*;
import ba.unsa.etf.NBP.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock private AttendanceRepository attendanceRepository;
    @Mock private CourseSessionRepository courseSessionRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private ProfessorRepository professorRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private DataSource dataSource;
    @Mock private Connection connection;
    @Mock private CallableStatement callableStatement;
    @Mock private AttendanceAlertService alertService;

    private AttendanceService service;

    private User studentUser;
    private User professorUser;
    private Student student;
    private Professor professor;

    @BeforeEach
    void setUp() throws Exception {
        service = new AttendanceService(attendanceRepository, courseSessionRepository, enrollmentRepository,
                studentRepository, professorRepository, courseRepository, dataSource);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareCall(any())).thenReturn(callableStatement);
                studentRepository, professorRepository, courseRepository, alertService);

        studentUser = new User(1L, "stud1", "pass", "Ana", "A", "ana@etf.ba", null, null, new Role(1L, "Student"));
        professorUser = new User(2L, "prof1", "pass", "Prof", "P", "prof@etf.ba", null, null, new Role(2L, "Professor"));

        student = new Student(10L, 1L, "IB2001", 5L, 2020L);
        professor = new Professor(20L, 2L, "Dr.", 1L, "A-1");
    }

    @Test
    void registerWithValidSessionCodeCallsPackage() throws Exception {
        CourseSession openSession = new CourseSession(100L, 999L, LocalDateTime.now().minusMinutes(10), null,
                "ATT-123456", 50L, null, null);
        Attendance attendance = new Attendance(555L, 10L, 100L, true, LocalDateTime.now(), null);

        when(studentRepository.findByUserId(1L)).thenReturn(Optional.of(student));
        when(courseSessionRepository.findBySessionCode("ATT-123456")).thenReturn(Optional.of(openSession));
        when(attendanceRepository.findByStudentIdAndCourseSessionId(10L, 100L)).thenReturn(Optional.of(attendance));

        Attendance created = service.registerAttendance("ATT-123456", studentUser);

        verify(callableStatement).execute();
        assertNotNull(created);
        assertEquals(555L, created.getId());
        assertTrue(created.isPresent());
    }

    @Test
    void registerWithBlankCodeReturns400() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.registerAttendance("", studentUser));

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void registerWhenUserIsNotStudentReturns403() {
        when(studentRepository.findByUserId(1L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.registerAttendance("ATT-123456", studentUser));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void closeSessionAutoAbsentInsertsMissingRecords() {
        CourseSession closedSession = new CourseSession(100L, 999L, LocalDateTime.now().minusMinutes(60),
                LocalDateTime.now(), "ATT-123456", 50L, null, null);

        when(attendanceRepository.autoInsertAbsentForMissingEnrolledStudents(999L, 100L)).thenReturn(2);

        int inserted = service.autoMarkAbsentForSession(closedSession);

        assertEquals(2, inserted);
        verify(attendanceRepository).autoInsertAbsentForMissingEnrolledStudents(999L, 100L);
    }

    @Test
    void closeSessionAutoAbsentInsertsZeroWhenAllAlreadyRegistered() {
        CourseSession closedSession = new CourseSession(100L, 999L, LocalDateTime.now().minusMinutes(60),
                LocalDateTime.now(), "ATT-123456", 50L, null, null);

        when(attendanceRepository.autoInsertAbsentForMissingEnrolledStudents(999L, 100L)).thenReturn(0);

        int inserted = service.autoMarkAbsentForSession(closedSession);

        assertEquals(0, inserted);
        verify(attendanceRepository).autoInsertAbsentForMissingEnrolledStudents(999L, 100L);
    }

    @Test
    void professorManualOverrideUpdatesIsPresent() {
        Attendance attendance = new Attendance(777L, 10L, 100L, false, null, null);
        CourseSession session = new CourseSession(100L, 999L, LocalDateTime.now().minusMinutes(60), null,
                "ATT-123456", 50L, null, null);
        Course course = new Course(999L, "DB", "NBP", 20L, 1L, "2025/2026", 2L, 6L);

        when(professorRepository.findByUserId(2L)).thenReturn(Optional.of(professor));
        when(attendanceRepository.findById(777L)).thenReturn(Optional.of(attendance));
        when(courseSessionRepository.findById(100L)).thenReturn(Optional.of(session));
        when(courseRepository.findById(999L)).thenReturn(Optional.of(course));

        Attendance updated = service.overrideAttendancePresence(777L, true, professorUser);

        assertTrue(updated.isPresent());
        verify(attendanceRepository).updateIsPresent(777L, true);
    }

    @Test
    void professorOverrideAnotherProfessorsSessionReturns403() {
        Attendance attendance = new Attendance(777L, 10L, 100L, false, null, null);
        CourseSession session = new CourseSession(100L, 999L, LocalDateTime.now().minusMinutes(60), null,
                "ATT-123456", 50L, null, null);
        Course course = new Course(999L, "DB", "NBP", 9999L, 1L, "2025/2026", 2L, 6L);

        when(professorRepository.findByUserId(2L)).thenReturn(Optional.of(professor));
        when(attendanceRepository.findById(777L)).thenReturn(Optional.of(attendance));
        when(courseSessionRepository.findById(100L)).thenReturn(Optional.of(session));
        when(courseRepository.findById(999L)).thenReturn(Optional.of(course));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.overrideAttendancePresence(777L, true, professorUser));

        assertEquals(403, ex.getStatusCode().value());
        verify(attendanceRepository, never()).updateIsPresent(any(), anyBoolean());
    }

    @Test
    void getAttendanceForSessionReturnsRecordsWhenProfessorOwnsSession() {
        CourseSession session = new CourseSession(100L, 999L, LocalDateTime.now().minusMinutes(60), null,
                "ATT-123456", 50L, null, null);
        Course course = new Course(999L, "DB", "NBP", 20L, 1L, "2025/2026", 2L, 6L);

        when(professorRepository.findByUserId(2L)).thenReturn(Optional.of(professor));
        when(courseSessionRepository.findById(100L)).thenReturn(Optional.of(session));
        when(courseRepository.findById(999L)).thenReturn(Optional.of(course));
        when(attendanceRepository.findSessionAttendanceWithStudentNames(100L))
                .thenReturn(List.of(new SessionAttendanceRecordResponse()));

        List<SessionAttendanceRecordResponse> result = service.getAttendanceForSession(100L, professorUser);

        assertEquals(1, result.size());
        verify(attendanceRepository).findSessionAttendanceWithStudentNames(100L);
    }

    @Test
    void studentCanOnlyViewOwnHistory() {
        when(studentRepository.findByUserId(1L)).thenReturn(Optional.of(student));
        when(attendanceRepository.findByStudentId(10L)).thenReturn(List.of());

        List<Attendance> ok = service.getAttendanceHistoryForStudent(10L, studentUser);
        assertNotNull(ok);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.getAttendanceHistoryForStudent(999L, studentUser));
        assertEquals(403, ex.getStatusCode().value());
    }
}