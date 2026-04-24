package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.dto.session.CourseSessionResponse;
import ba.unsa.etf.NBP.dto.session.OpenSessionRequest;
import ba.unsa.etf.NBP.model.*;
import ba.unsa.etf.NBP.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Session management for attendance registration.
 * <p>
 * Professors open a session to start accepting student check-ins via 6-digit code,
 * and close it when the class ends, triggering auto-marking of absent students.
 */
@Service
public class CourseSessionService {

    private final CourseSessionRepository courseSessionRepository;
    private final CourseRepository courseRepository;
    private final ProfessorRepository professorRepository;
    private final TimetableRepository timetableRepository;
    private final RoomRepository roomRepository;
    private final AttendanceService attendanceService;
    private final Random random = new Random();

    public CourseSessionService(CourseSessionRepository courseSessionRepository,
                                CourseRepository courseRepository,
                                ProfessorRepository professorRepository,
                                TimetableRepository timetableRepository,
                                RoomRepository roomRepository,
                                AttendanceService attendanceService) {
        this.courseSessionRepository = courseSessionRepository;
        this.courseRepository = courseRepository;
        this.professorRepository = professorRepository;
        this.timetableRepository = timetableRepository;
        this.roomRepository = roomRepository;
        this.attendanceService = attendanceService;
    }

    /**
     * Returns every course session.
     *
     * @return all sessions
     */
    public List<CourseSession> findAll() {
        return courseSessionRepository.findAll();
    }

    /**
     * Looks up a course session by ID.
     *
     * @param id session ID
     * @return the session, or {@link Optional#empty()} if missing
     */
    public Optional<CourseSession> findById(Long id) {
        return courseSessionRepository.findById(id);
    }

    /**
     * Inserts a new course session (low level).
     *
     * @param courseSession session to insert
     */
    public void save(CourseSession courseSession) {
        courseSessionRepository.save(courseSession);
    }

    /**
     * Updates a course session.
     *
     * @param courseSession session with updated fields (ID required)
     */
    public void update(CourseSession courseSession) {
        courseSessionRepository.update(courseSession);
    }

    /**
     * Deletes a course session by ID.
     *
     * @param id session ID
     */
    public void deleteById(Long id) {
        courseSessionRepository.deleteById(id);
    }

    /**
     * Opens a new attendance session on a course the caller owns.
     * <p>
     * Generates a unique 6-digit session code and optionally fills the room from a timetable.
     *
     * @param courseId course the session belongs to
     * @param request optional body specifying {@code timetableId} or {@code roomId}
     * @param currentUser the authenticated professor
     * @return the newly opened session with its assigned code
     * @throws ResponseStatusException 403 if caller is not the course owner,
     *         400 if already an open session or invalid room/timetable,
     *         404 if course not found
     */
    public CourseSessionResponse openSession(Long courseId, OpenSessionRequest request, User currentUser) {
        Professor professor = getProfessorOrThrow(currentUser);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (!course.getProfessorId().equals(professor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this course");
        }

        if (courseSessionRepository.findOpenByCourseId(courseId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An open session already exists for this course");
        }

        Long roomId;
        Long timetableId = null;

        if (request != null && request.getTimetableId() != null) {
            Timetable timetable = timetableRepository.findById(request.getTimetableId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Timetable not found"));
            roomId = timetable.getRoomId();
            timetableId = timetable.getId();
        } else if (request != null && request.getRoomId() != null) {
            roomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room not found"));
            roomId = request.getRoomId();
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Either timetableId or roomId must be provided");
        }

        String sessionCode = generateUniqueSessionCode();

        CourseSession session = new CourseSession();
        session.setCourseId(courseId);
        session.setSessionStartTime(LocalDateTime.now());
        session.setSessionEndTime(null);
        session.setSessionCode(sessionCode);
        session.setRoomId(roomId);
        session.setTimetableId(timetableId);

        Long id = courseSessionRepository.saveAndReturnId(session);
        session.setId(id);

        Room room = roomRepository.findById(roomId).orElse(null);
        return toResponse(session, room);
    }

    /**
     * Closes an open attendance session and auto-marks all absent students.
     *
     * @param sessionId session ID
     * @param currentUser the authenticated professor
     * @return the closed session with updated end time
     * @throws ResponseStatusException 403 if caller does not own the session,
     *         400 if session is already closed,
     *         404 if session or course not found
     */
    public CourseSessionResponse closeSession(Long sessionId, User currentUser) {
        Professor professor = getProfessorOrThrow(currentUser);

        CourseSession session = courseSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        Course course = courseRepository.findById(session.getCourseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (!course.getProfessorId().equals(professor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this session");
        }

        if (session.getSessionEndTime() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session is already closed");
        }

        session.setSessionEndTime(LocalDateTime.now());
        courseSessionRepository.update(session);

        attendanceService.autoMarkAbsentForSession(session);

        Room room = roomRepository.findById(session.getRoomId()).orElse(null);
        return toResponse(session, room);
    }

/**
     * Returns the full session history for a course.
     *
     * @param courseId course ID
     * @param currentUser the authenticated professor
     * @return sessions for the course ordered by start time
     * @throws ResponseStatusException 403 if caller does not own the course,
     *         404 if course not found
     */
    public List<CourseSessionResponse> getCourseSessionHistory(Long courseId, User currentUser) {
        Professor professor = getProfessorOrThrow(currentUser);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (!course.getProfessorId().equals(professor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this course");
        }

        List<CourseSession> sessions = courseSessionRepository.findByCourseIdOrderByStartTime(courseId);

        return sessions.stream().map(session -> {
            Room room = roomRepository.findById(session.getRoomId()).orElse(null);
            return toResponse(session, room);
        }).collect(Collectors.toList());
    }

    /**
     * Extracts the professor record from the authenticated user.
     *
     * @param currentUser the authenticated user
     * @return the professor record
     * @throws ResponseStatusException 403 if user is not a professor
     */
    private Professor getProfessorOrThrow(User currentUser) {
        return professorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a professor"));
    }

    /**
     * Generates a unique 6-digit session code, retrying until a collision-free code is found.
     *
     * @return a unique session code
     */
    private String generateUniqueSessionCode() {
        String code;
        do {
            code = String.format("%06d", random.nextInt(1_000_000));
        } while (courseSessionRepository.existsBySessionCode(code));
        return code;
    }

    /**
     * Converts a session and its room to a response DTO.
     *
     * @param session the session
     * @param room the room (may be null)
     * @return response DTO with room details if available
     */
    private CourseSessionResponse toResponse(CourseSession session, Room room) {
        CourseSessionResponse response = new CourseSessionResponse();
        response.setId(session.getId());
        response.setCourseId(session.getCourseId());
        response.setSessionStartTime(session.getSessionStartTime());
        response.setSessionEndTime(session.getSessionEndTime());
        response.setSessionCode(session.getSessionCode());
        response.setRoomId(session.getRoomId());
        response.setTimetableId(session.getTimetableId());
        response.setSessionType(session.getSessionType());
        if (room != null) {
            response.setRoomName(room.getName());
            response.setRoomBuilding(room.getBuilding());
        }
        return response;
    }
}
