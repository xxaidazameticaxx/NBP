package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.dto.session.CourseSessionResponse;
import ba.unsa.etf.NBP.dto.session.OpenSessionRequest;
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
class CourseSessionServiceTest {

    @Mock private CourseSessionRepository courseSessionRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private ProfessorRepository professorRepository;
    @Mock private TimetableRepository timetableRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private AttendanceService attendanceService;
    @Mock private DataSource dataSource;
    @Mock private Connection connection;
    @Mock private CallableStatement callableStatement;

    private CourseSessionService service;

    private User professorUser;
    private User otherProfessorUser;
    private Professor professor;
    private Professor otherProfessor;
    private Course course;
    private Room room;
    private Timetable timetable;

    @BeforeEach
    void setUp() throws Exception {
        service = new CourseSessionService(courseSessionRepository, courseRepository,
                professorRepository, timetableRepository, roomRepository, attendanceService, dataSource);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareCall(any())).thenReturn(callableStatement);
        when(callableStatement.getLong(4)).thenReturn(1L);
        when(callableStatement.getString(5)).thenReturn("ATT-000001");

        professorUser = new User(1L, "prof1", "pass", "John", "Doe", "john@etf.ba", null, null, new Role(2L, "Professor"));
        otherProfessorUser = new User(2L, "prof2", "pass", "Jane", "Smith", "jane@etf.ba", null, null, new Role(2L, "Professor"));

        professor = new Professor(10L, 1L, "Dr.", 1L, "A-101");
        otherProfessor = new Professor(20L, 2L, "Dr.", 1L, "B-202");

        course = new Course(100L, "Databases", "NBP", 10L, 1L, "2025/2026", 2L, 6L);

        room = new Room(50L, "A1-01", "Building A");

        timetable = new Timetable(200L, 100L, 50L, "MONDAY", null, null, null, null);
    }

    @Test
    void openSessionForOwnCourseReturnsSessionWithUniqueCode() throws Exception {
        when(professorRepository.findByUserId(1L)).thenReturn(Optional.of(professor));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(timetableRepository.findById(200L)).thenReturn(Optional.of(timetable));

        OpenSessionRequest request = new OpenSessionRequest();
        request.setTimetableId(200L);

        CourseSessionResponse response = service.openSession(100L, request, professorUser);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(100L, response.getCourseId());
        assertNotNull(response.getSessionCode());
        verify(callableStatement).execute();
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
    void openSessionWithoutRoomOrTimetableThrowsBadRequest() {
        when(professorRepository.findByUserId(1L)).thenReturn(Optional.of(professor));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.openSession(100L, null, professorUser));

        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void closeOpenSessionCallsPackage() throws Exception {
        CourseSession openSession = new CourseSession(1L, 100L, LocalDateTime.now().minusHours(1),
                null, "ATT-000001", 50L, 200L, null);
        CourseSession closedSession = new CourseSession(1L, 100L, LocalDateTime.now().minusHours(1),
                LocalDateTime.now(), "ATT-000001", 50L, 200L, null);

        when(professorRepository.findByUserId(1L)).thenReturn(Optional.of(professor));
        when(courseSessionRepository.findById(1L))
                .thenReturn(Optional.of(openSession))
                .thenReturn(Optional.of(closedSession));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(roomRepository.findById(50L)).thenReturn(Optional.of(room));

        CourseSessionResponse response = service.closeSession(1L, professorUser);

        verify(callableStatement).execute();
        assertNotNull(response.getSessionEndTime());
    }

    @Test
    void closeAnotherProfessorsSessionThrowsForbidden() {
        CourseSession openSession = new CourseSession(1L, 100L, LocalDateTime.now().minusHours(1),
                null, "ATT-000001", 50L, 200L, null);

        when(professorRepository.findByUserId(2L)).thenReturn(Optional.of(otherProfessor));
        when(courseSessionRepository.findById(1L)).thenReturn(Optional.of(openSession));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(course));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.closeSession(1L, otherProfessorUser));

        assertEquals(403, ex.getStatusCode().value());
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
}