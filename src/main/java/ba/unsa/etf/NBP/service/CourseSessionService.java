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

    public List<CourseSession> findAll() {
        return courseSessionRepository.findAll();
    }

    public Optional<CourseSession> findById(Long id) {
        return courseSessionRepository.findById(id);
    }

    public void save(CourseSession courseSession) {
        courseSessionRepository.save(courseSession);
    }

    public void update(CourseSession courseSession) {
        courseSessionRepository.update(courseSession);
    }

    public void deleteById(Long id) {
        courseSessionRepository.deleteById(id);
    }

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

    private Professor getProfessorOrThrow(User currentUser) {
        return professorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a professor"));
    }

    private String generateUniqueSessionCode() {
        String code;
        do {
            code = String.format("%06d", random.nextInt(1_000_000));
        } while (courseSessionRepository.existsBySessionCode(code));
        return code;
    }

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
