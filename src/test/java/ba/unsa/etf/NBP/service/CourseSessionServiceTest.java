package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.dto.session.CourseSessionResponse;
import ba.unsa.etf.NBP.dto.session.OpenSessionRequest;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseSessionServiceTest {

    @Mock private CourseSessionRepository courseSessionRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private ProfessorRepository professorRepository;
    @Mock private TimetableRepository timetableRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private AttendanceService attendanceService;

    private CourseSessionService service;

    private User professorUser;
    private User otherProfessorUser;
    private Professor professor;
    private Professor otherProfessor;
    private Course course;
    private Room room;
    private Timetable timetable;

    @BeforeEach
    void setUp() {
        service = new CourseSessionService(courseSessionRepository, courseRepository,
                professorRepository, timetableRepository, roomRepository, attendanceService);

        professorUser = new User(1L, "prof1", "pass", "John", "Doe", "john@etf.ba", null, null, new Role(2L, "Professor"));
        otherProfessorUser = new User(2L, "prof2", "pass", "Jane", "Smith", "jane@etf.ba", null, null, new Role(2L, "Professor"));

        professor = new Professor(10L, 1L, "Dr.", 1L, "A-101");
        otherProfessor = new Professor(20L, 2L, "Dr.", 1L, "B-202");

        course = new Course(100L, "Databases", "NBP", 10L, 1L, "2025/2026", 2L, 6L);

        room = new Room(50L, "A1-01", "Building A");

        timetable = new Timetable(200L, 100L, 50L, "MONDAY", null, null, null, null);
    }

    @Test
    void openSessionForOwnCourseReturnsSessionWithUniqueCode() {
        when(professorRepository.findByUserId(1L)).thenReturn(Optional.of(professor));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(courseSessionRepository.findOpenByCourseId(100L)).thenReturn(Optional.empty());
        when(timetableRepository.findById(200L)).thenReturn(Optional.of(timetable));
        when(courseSessionRepository.existsBySessionCode(anyString())).thenReturn(false);
        when(courseSessionRepository.saveAndReturnId(any(CourseSession.class))).thenReturn(1L);
        when(roomRepository.findById(50L)).thenReturn(Optional.of(room));

        OpenSessionRequest request = new OpenSessionRequest();
        request.setTimetableId(200L);

        CourseSessionResponse response = service.openSession(100L, request, professorUser);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(100L, response.getCourseId());
        assertNotNull(response.getSessionCode());
        assertEquals(6, response.getSessionCode().length());
        assertTrue(response.getSessionCode().matches("\\d{6}"));
        assertNull(response.getSessionEndTime());
        assertEquals("A1-01", response.getRoomName());
        assertEquals("Building A", response.getRoomBuilding());
        assertEquals(200L, response.getTimetableId());

        verify(courseSessionRepository).saveAndReturnId(any(CourseSession.class));
    }

    @Test
    void openSessionForAnotherProfessorsCourseThrowsForbidden() {
        when(professorRepository.findByUserId(2L)).thenReturn(Optional.of(otherProfessor));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));

        OpenSessionRequest request = new OpenSessionRequest();
        request.setTimetableId(200L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.openSession(100L, request, otherProfessorUser));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void openSessionWhenAlreadyActiveThrowsBadRequest() {
        when(professorRepository.findByUserId(1L)).thenReturn(Optional.of(professor));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));

        CourseSession existingOpen = new CourseSession();
        existingOpen.setId(99L);
        when(courseSessionRepository.findOpenByCourseId(100L)).thenReturn(Optional.of(existingOpen));

        OpenSessionRequest request = new OpenSessionRequest();
        request.setTimetableId(200L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.openSession(100L, request, professorUser));

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void openSessionWithTimetableAutoFillsRoomFromTimetable() {
        when(professorRepository.findByUserId(1L)).thenReturn(Optional.of(professor));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(courseSessionRepository.findOpenByCourseId(100L)).thenReturn(Optional.empty());
        when(timetableRepository.findById(200L)).thenReturn(Optional.of(timetable));
        when(courseSessionRepository.existsBySessionCode(anyString())).thenReturn(false);
        when(courseSessionRepository.saveAndReturnId(any(CourseSession.class))).thenReturn(1L);
        when(roomRepository.findById(50L)).thenReturn(Optional.of(room));

        OpenSessionRequest request = new OpenSessionRequest();
        request.setTimetableId(200L);

        service.openSession(100L, request, professorUser);

        ArgumentCaptor<CourseSession> captor = ArgumentCaptor.forClass(CourseSession.class);
        verify(courseSessionRepository).saveAndReturnId(captor.capture());

        CourseSession saved = captor.getValue();
        assertEquals(50L, saved.getRoomId());
        assertEquals(200L, saved.getTimetableId());
    }

    @Test
    void closeOpenSessionSetsEndTime() {
        CourseSession openSession = new CourseSession(1L, 100L, LocalDateTime.now().minusHours(1),
                null, "123456", 50L, 200L, null);

        when(professorRepository.findByUserId(1L)).thenReturn(Optional.of(professor));
        when(courseSessionRepository.findById(1L)).thenReturn(Optional.of(openSession));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(roomRepository.findById(50L)).thenReturn(Optional.of(room));

        CourseSessionResponse response = service.closeSession(1L, professorUser);

        assertNotNull(response.getSessionEndTime());
        verify(courseSessionRepository).update(any(CourseSession.class));
        verify(attendanceService).autoMarkAbsentForSession(any(CourseSession.class));
    }

    @Test
    void closeAlreadyClosedSessionThrowsBadRequest() {
        CourseSession closedSession = new CourseSession(1L, 100L, LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1), "123456", 50L, 200L, null);

        when(professorRepository.findByUserId(1L)).thenReturn(Optional.of(professor));
        when(courseSessionRepository.findById(1L)).thenReturn(Optional.of(closedSession));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.closeSession(1L, professorUser));

        assertEquals(400, ex.getStatusCode().value());
        verify(attendanceService, never()).autoMarkAbsentForSession(any(CourseSession.class));
    }

    @Test
    void closeAnotherProfessorsSessionThrowsForbidden() {
        CourseSession openSession = new CourseSession(1L, 100L, LocalDateTime.now().minusHours(1),
                null, "123456", 50L, 200L, null);

        when(professorRepository.findByUserId(2L)).thenReturn(Optional.of(otherProfessor));
        when(courseSessionRepository.findById(1L)).thenReturn(Optional.of(openSession));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.closeSession(1L, otherProfessorUser));

        assertEquals(403, ex.getStatusCode().value());
        verify(attendanceService, never()).autoMarkAbsentForSession(any(CourseSession.class));
    }

    @Test
    void getCourseSessionHistoryReturnsAllSessionsOrdered() {
        CourseSession s1 = new CourseSession(1L, 100L, LocalDateTime.of(2025, 3, 1, 9, 0),
                LocalDateTime.of(2025, 3, 1, 10, 0), "111111", 50L, null, null);
        CourseSession s2 = new CourseSession(2L, 100L, LocalDateTime.of(2025, 3, 8, 9, 0),
                LocalDateTime.of(2025, 3, 8, 10, 0), "222222", 50L, null, null);
        CourseSession s3 = new CourseSession(3L, 100L, LocalDateTime.of(2025, 3, 15, 9, 0),
                null, "333333", 50L, null, null);

        when(professorRepository.findByUserId(1L)).thenReturn(Optional.of(professor));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(courseSessionRepository.findByCourseIdOrderByStartTime(100L)).thenReturn(List.of(s1, s2, s3));
        when(roomRepository.findById(50L)).thenReturn(Optional.of(room));

        List<CourseSessionResponse> result = service.getCourseSessionHistory(100L, professorUser);

        assertEquals(3, result.size());
        assertTrue(result.get(0).getSessionStartTime().isBefore(result.get(1).getSessionStartTime()));
        assertTrue(result.get(1).getSessionStartTime().isBefore(result.get(2).getSessionStartTime()));
    }

    @Test
    void twoProfessorsOpenSessionsSimultaneouslyGetUniqueCodes() {
        Course otherCourse = new Course(101L, "Algorithms", "ALG", 20L, 1L, "2025/2026", 2L, 5L);
        Timetable otherTimetable = new Timetable(201L, 101L, 50L, "TUESDAY", null, null, null, null);

        when(professorRepository.findByUserId(1L)).thenReturn(Optional.of(professor));
        when(professorRepository.findByUserId(2L)).thenReturn(Optional.of(otherProfessor));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(courseRepository.findById(101L)).thenReturn(Optional.of(otherCourse));
        when(courseSessionRepository.findOpenByCourseId(100L)).thenReturn(Optional.empty());
        when(courseSessionRepository.findOpenByCourseId(101L)).thenReturn(Optional.empty());
        when(timetableRepository.findById(200L)).thenReturn(Optional.of(timetable));
        when(timetableRepository.findById(201L)).thenReturn(Optional.of(otherTimetable));
        when(courseSessionRepository.existsBySessionCode(anyString())).thenReturn(false);
        when(courseSessionRepository.saveAndReturnId(any(CourseSession.class))).thenReturn(1L, 2L);
        when(roomRepository.findById(50L)).thenReturn(Optional.of(room));

        OpenSessionRequest req1 = new OpenSessionRequest();
        req1.setTimetableId(200L);
        OpenSessionRequest req2 = new OpenSessionRequest();
        req2.setTimetableId(201L);

        CourseSessionResponse response1 = service.openSession(100L, req1, professorUser);
        CourseSessionResponse response2 = service.openSession(101L, req2, otherProfessorUser);

        assertNotNull(response1.getSessionCode());
        assertNotNull(response2.getSessionCode());
        assertEquals(6, response1.getSessionCode().length());
        assertEquals(6, response2.getSessionCode().length());
        verify(courseSessionRepository, times(2)).saveAndReturnId(any(CourseSession.class));
    }
}
