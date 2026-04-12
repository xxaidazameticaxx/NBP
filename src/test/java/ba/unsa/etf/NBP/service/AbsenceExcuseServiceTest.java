package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.model.*;
import ba.unsa.etf.NBP.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

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

    private AbsenceExcuseService service;

    // shared fixtures
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
    void setUp() {
        service = new AbsenceExcuseService(
                absenceExcuseRepository,
                attendanceRepository,
                courseSessionRepository,
                courseRepository,
                professorRepository,
                studentRepository,
                notificationService);

        studentUser      = new User(10L, "student1", "pass", "Ana", "Beg", "ana@etf.ba", null, null, new Role(1L, "Student"));
        professorUser    = new User(20L, "prof1",    "pass", "John", "Doe", "j@etf.ba",  null, null, new Role(2L, "Professor"));
        otherProfessorUser = new User(21L, "prof2",  "pass", "Jane", "Smith","s@etf.ba", null, null, new Role(2L, "Professor"));

        student         = new Student(1L, 10L, "IB190001", 1L, 2019L);
        professor       = new Professor(5L, 20L, "Dr.", 1L, "A-101");
        otherProfessor  = new Professor(6L, 21L, "Dr.", 1L, "B-202");

        course  = new Course(100L, "Databases", "NBP", 5L, 1L, "2025/2026", 2L, 6L);
        session = new CourseSession(200L, 100L,
                LocalDateTime.of(2026, 3, 10, 9, 0),
                LocalDateTime.of(2026, 3, 10, 10, 0),
                "123456", 50L, null, null);

        pendingExcuse = new AbsenceExcuse(99L, 1L, 200L, "I was sick", LocalDateTime.now(), "PENDING", null);
    }

    // ── Scenario 1: Submit excuse when absent ─────────────────────────────────

    @Test
    void submitExcuse_whenStudentIsAbsent_returnsPendingExcuse() {
        Attendance absentRecord = new Attendance(1L, 1L, 200L, false, LocalDateTime.now(), null);

        when(studentRepository.findByUserId(10L)).thenReturn(Optional.of(student));
        when(attendanceRepository.findByStudentIdAndCourseSessionId(1L, 200L)).thenReturn(Optional.of(absentRecord));
        when(absenceExcuseRepository.findByStudentIdAndCourseSessionId(1L, 200L)).thenReturn(Optional.empty());
        when(absenceExcuseRepository.saveAndReturnId(any())).thenReturn(99L);

        AbsenceExcuse result = service.submitExcuse(200L, "I was sick", studentUser);

        assertEquals(99L, result.getId());
        assertEquals("PENDING", result.getStatus());
        assertEquals(1L, result.getStudentId());
        assertEquals(200L, result.getCourseSessionId());
        assertNotNull(result.getSubmittedAt());
        verify(absenceExcuseRepository).saveAndReturnId(any(AbsenceExcuse.class));
    }

    // ── Scenario 2: Submit excuse when present ────────────────────────────────

    @Test
    void submitExcuse_whenStudentIsPresent_throwsBadRequest() {
        Attendance presentRecord = new Attendance(1L, 1L, 200L, true, LocalDateTime.now(), null);

        when(studentRepository.findByUserId(10L)).thenReturn(Optional.of(student));
        when(attendanceRepository.findByStudentIdAndCourseSessionId(1L, 200L)).thenReturn(Optional.of(presentRecord));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.submitExcuse(200L, "reason", studentUser));

        assertEquals(400, ex.getStatusCode().value());
        verify(absenceExcuseRepository, never()).saveAndReturnId(any());
    }

    // ── Scenario 3: Submit duplicate excuse ───────────────────────────────────

    @Test
    void submitExcuse_whenDuplicateExists_throwsBadRequest() {
        Attendance absentRecord = new Attendance(1L, 1L, 200L, false, LocalDateTime.now(), null);

        when(studentRepository.findByUserId(10L)).thenReturn(Optional.of(student));
        when(attendanceRepository.findByStudentIdAndCourseSessionId(1L, 200L)).thenReturn(Optional.of(absentRecord));
        when(absenceExcuseRepository.findByStudentIdAndCourseSessionId(1L, 200L)).thenReturn(Optional.of(pendingExcuse));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.submitExcuse(200L, "reason", studentUser));

        assertEquals(400, ex.getStatusCode().value());
        verify(absenceExcuseRepository, never()).saveAndReturnId(any());
    }

    // ── Scenario 4: Professor approves pending excuse ─────────────────────────

    @Test
    void approveExcuse_setStatusApprovedAndCreatesNotification() {
        when(professorRepository.findByUserId(20L)).thenReturn(Optional.of(professor));
        when(absenceExcuseRepository.findById(99L)).thenReturn(Optional.of(pendingExcuse));
        when(courseSessionRepository.findById(200L)).thenReturn(Optional.of(session));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        service.approveExcuse(99L, professorUser);

        ArgumentCaptor<AbsenceExcuse> captor = ArgumentCaptor.forClass(AbsenceExcuse.class);
        verify(absenceExcuseRepository).update(captor.capture());
        assertEquals("APPROVED", captor.getValue().getStatus());
        assertEquals(20L, captor.getValue().getReviewedBy());

        verify(notificationService).save(argThat(n ->
                n.getUserId().equals(10L) &&
                "Excuse Approved".equals(n.getTitle()) &&
                n.getMessage().contains("Databases") &&
                "EXCUSE_APPROVED".equals(n.getNotificationType()) &&
                n.getCourseSessionId().equals(200L)));
    }

    // ── Scenario 5: Professor rejects pending excuse ──────────────────────────

    @Test
    void rejectExcuse_setStatusRejectedAndCreatesNotification() {
        when(professorRepository.findByUserId(20L)).thenReturn(Optional.of(professor));
        when(absenceExcuseRepository.findById(99L)).thenReturn(Optional.of(pendingExcuse));
        when(courseSessionRepository.findById(200L)).thenReturn(Optional.of(session));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student));

        service.rejectExcuse(99L, professorUser);

        ArgumentCaptor<AbsenceExcuse> captor = ArgumentCaptor.forClass(AbsenceExcuse.class);
        verify(absenceExcuseRepository).update(captor.capture());
        assertEquals("REJECTED", captor.getValue().getStatus());
        assertEquals(20L, captor.getValue().getReviewedBy());

        verify(notificationService).save(argThat(n ->
                n.getUserId().equals(10L) &&
                "Excuse Rejected".equals(n.getTitle()) &&
                n.getMessage().contains("Databases") &&
                "EXCUSE_REJECTED".equals(n.getNotificationType()) &&
                n.getCourseSessionId().equals(200L)));
    }

    // ── Scenario 6: Approve already reviewed excuse ───────────────────────────

    @Test
    void approveExcuse_whenAlreadyReviewed_throwsBadRequest() {
        AbsenceExcuse alreadyApproved = new AbsenceExcuse(99L, 1L, 200L, "sick",
                LocalDateTime.now(), "APPROVED", 20L);

        when(professorRepository.findByUserId(20L)).thenReturn(Optional.of(professor));
        when(absenceExcuseRepository.findById(99L)).thenReturn(Optional.of(alreadyApproved));
        when(courseSessionRepository.findById(200L)).thenReturn(Optional.of(session));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.approveExcuse(99L, professorUser));

        assertEquals(400, ex.getStatusCode().value());
        verify(absenceExcuseRepository, never()).update(any());
        verify(notificationService, never()).save(any());
    }

    // ── Scenario 7: Professor approves excuse from another professor's session ─

    @Test
    void approveExcuse_fromAnotherProfessorsSession_throwsForbidden() {
        when(professorRepository.findByUserId(21L)).thenReturn(Optional.of(otherProfessor));
        when(absenceExcuseRepository.findById(99L)).thenReturn(Optional.of(pendingExcuse));
        when(courseSessionRepository.findById(200L)).thenReturn(Optional.of(session));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course)); // owned by professor(5L), not otherProfessor(6L)

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.approveExcuse(99L, otherProfessorUser));

        assertEquals(403, ex.getStatusCode().value());
        verify(absenceExcuseRepository, never()).update(any());
    }

    // ── Scenario 8: Get student's excuses ─────────────────────────────────────

    @Test
    void findByStudentId_returnsAllExcusesWithStatuses() {
        List<AbsenceExcuse> excuses = List.of(
                new AbsenceExcuse(1L, 1L, 200L, "sick",     LocalDateTime.now(), "PENDING",  null),
                new AbsenceExcuse(2L, 1L, 201L, "emergency",LocalDateTime.now(), "APPROVED", 20L),
                new AbsenceExcuse(3L, 1L, 202L, "late bus",  LocalDateTime.now(), "REJECTED", 20L)
        );

        when(absenceExcuseRepository.findByStudentId(1L)).thenReturn(excuses);

        List<AbsenceExcuse> result = service.findByStudentId(1L);

        assertEquals(3, result.size());
        assertEquals("PENDING",  result.get(0).getStatus());
        assertEquals("APPROVED", result.get(1).getStatus());
        assertEquals("REJECTED", result.get(2).getStatus());
    }

    // ── Scenario 9: Get pending excuses for professor ─────────────────────────

    @Test
    void findPendingByProfessor_returnsOnlyPendingFromOwnCourses() {
        List<AbsenceExcuse> pending = List.of(
                new AbsenceExcuse(1L, 1L, 200L, "sick", LocalDateTime.now(), "PENDING", null),
                new AbsenceExcuse(2L, 2L, 201L, "travel",LocalDateTime.now(), "PENDING", null)
        );

        when(professorRepository.findByUserId(20L)).thenReturn(Optional.of(professor));
        when(absenceExcuseRepository.findPendingByProfessorId(5L)).thenReturn(pending);

        List<AbsenceExcuse> result = service.findPendingByProfessor(professorUser);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(e -> "PENDING".equals(e.getStatus())));
    }

    // ── Scenario 10: Get excuses for a session ────────────────────────────────

    @Test
    void findByCourseSessionId_returnsAllExcusesTiedToSession() {
        List<AbsenceExcuse> excuses = List.of(
                new AbsenceExcuse(1L, 1L, 200L, "sick",    LocalDateTime.now(), "PENDING",  null),
                new AbsenceExcuse(2L, 2L, 200L, "family",  LocalDateTime.now(), "APPROVED", 20L)
        );

        when(absenceExcuseRepository.findByCourseSessionId(200L)).thenReturn(excuses);

        List<AbsenceExcuse> result = service.findByCourseSessionId(200L);

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(e -> e.getCourseSessionId().equals(200L)));
    }
}




