package ba.unsa.etf.NBP.service;

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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbsenceExcuseServiceTest {

    @Mock private AbsenceExcuseRepository absenceExcuseRepository;
    @Mock private AttendanceRepository attendanceRepository;
    @Mock private CourseSessionRepository courseSessionRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private ProfessorRepository professorRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private NotificationService notificationService;
    @Mock private DataSource dataSource;
    @Mock private Connection connection;
    @Mock private CallableStatement callableStatement;

    private AbsenceExcuseService service;

    private User studentUser;
    private User professorUser;
    private User otherProfessorUser;
    private Student student;
    private Professor professor;
    private Professor otherProfessor;
    private Course course;
    private CourseSession session;
    private AbsenceExcuse pendingExcuse;

    @BeforeEach
    void setUp() throws Exception {
        service = new AbsenceExcuseService(
                absenceExcuseRepository,
                attendanceRepository,
                courseSessionRepository,
                courseRepository,
                professorRepository,
                studentRepository,
                notificationService,
                dataSource);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareCall(any())).thenReturn(callableStatement);
        when(callableStatement.getLong(5)).thenReturn(99L);

        studentUser        = new User(10L, "student1", "pass", "Ana", "Beg", "ana@etf.ba", null, null, new Role(1L, "Student"));
        professorUser      = new User(20L, "prof1", "pass", "John", "Doe", "j@etf.ba", null, null, new Role(2L, "Professor"));
        otherProfessorUser = new User(21L, "prof2", "pass", "Jane", "Smith", "s@etf.ba", null, null, new Role(2L, "Professor"));

        student        = new Student(1L, 10L, "IB190001", 1L, 2019L);
        professor      = new Professor(5L, 20L, "Dr.", 1L, "A-101");
        otherProfessor = new Professor(6L, 21L, "Dr.", 1L, "B-202");

        course  = new Course(100L, "Databases", "NBP", 5L, 1L, "2025/2026", 2L, 6L);
        session = new CourseSession(200L, 100L,
                LocalDateTime.of(2026, 3, 10, 9, 0),
                LocalDateTime.of(2026, 3, 10, 10, 0),
                "123456", 50L, null, null);

        pendingExcuse = new AbsenceExcuse(99L, 1L, 200L, "I was sick", LocalDateTime.now(), "PENDING", null, null, null);
    }

    @Test
    void submitExcuse_whenStudentExists_callsPackageAndReturnsExcuse() throws Exception {
        when(studentRepository.findByUserId(10L)).thenReturn(Optional.of(student));
        when(absenceExcuseRepository.findById(99L)).thenReturn(Optional.of(pendingExcuse));

        AbsenceExcuse result = service.submitExcuse(200L, "I was sick", null, null, studentUser);

        verify(callableStatement).execute();
        assertNotNull(result);
        assertEquals(99L, result.getId());
        assertEquals("PENDING", result.getStatus());
    }

    @Test
    void submitExcuse_whenUserIsNotStudent_throwsForbidden() {
        when(studentRepository.findByUserId(10L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.submitExcuse(200L, "reason", null, null, studentUser));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void approveExcuse_callsPackageWithCorrectParams() throws Exception {
        when(professorRepository.findByUserId(20L)).thenReturn(Optional.of(professor));

        service.approveExcuse(99L, professorUser);

        verify(callableStatement).execute();
        verify(callableStatement).setLong(1, 99L);
        verify(callableStatement).setLong(2, 5L);
    }

    @Test
    void approveExcuse_whenUserIsNotProfessor_throwsForbidden() {
        when(professorRepository.findByUserId(20L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.approveExcuse(99L, professorUser));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void rejectExcuse_callsPackageWithCorrectParams() throws Exception {
        when(professorRepository.findByUserId(20L)).thenReturn(Optional.of(professor));

        service.rejectExcuse(99L, professorUser);

        verify(callableStatement).execute();
        verify(callableStatement).setLong(1, 99L);
        verify(callableStatement).setLong(2, 5L);
    }

    @Test
    void rejectExcuse_whenUserIsNotProfessor_throwsForbidden() {
        when(professorRepository.findByUserId(20L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.rejectExcuse(99L, professorUser));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void findByStudentId_returnsAllExcusesWithStatuses() {
        List<AbsenceExcuse> excuses = List.of(
                new AbsenceExcuse(1L, 1L, 200L, "sick",      LocalDateTime.now(), "PENDING",  null, null, null),
                new AbsenceExcuse(2L, 1L, 201L, "emergency", LocalDateTime.now(), "APPROVED", 20L,  null, null),
                new AbsenceExcuse(3L, 1L, 202L, "late bus",  LocalDateTime.now(), "REJECTED", 20L,  null, null)
        );

        when(absenceExcuseRepository.findByStudentId(1L)).thenReturn(excuses);

        List<AbsenceExcuse> result = service.findByStudentId(1L);

        assertEquals(3, result.size());
        assertEquals("PENDING",  result.get(0).getStatus());
        assertEquals("APPROVED", result.get(1).getStatus());
        assertEquals("REJECTED", result.get(2).getStatus());
    }

    @Test
    void findPendingByProfessor_returnsOnlyPendingFromOwnCourses() {
        List<AbsenceExcuse> pending = List.of(
                new AbsenceExcuse(1L, 1L, 200L, "sick",   LocalDateTime.now(), "PENDING", null, null, null),
                new AbsenceExcuse(2L, 2L, 201L, "travel", LocalDateTime.now(), "PENDING", null, null, null)
        );

        when(professorRepository.findByUserId(20L)).thenReturn(Optional.of(professor));
        when(absenceExcuseRepository.findPendingByProfessorId(5L)).thenReturn(pending);

        List<AbsenceExcuse> result = service.findPendingByProfessor(professorUser);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(e -> "PENDING".equals(e.getStatus())));
    }

    @Test
    void findByCourseSessionId_returnsAllExcusesTiedToSession() {
        List<AbsenceExcuse> excuses = List.of(
                new AbsenceExcuse(1L, 1L, 200L, "sick",   LocalDateTime.now(), "PENDING",  null, null, null),
                new AbsenceExcuse(2L, 2L, 200L, "family", LocalDateTime.now(), "APPROVED", 20L,  null, null)
        );

        when(absenceExcuseRepository.findByCourseSessionId(200L)).thenReturn(excuses);

        List<AbsenceExcuse> result = service.findByCourseSessionId(200L);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(e -> e.getCourseSessionId().equals(200L)));
    }
}