package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.model.*;
import ba.unsa.etf.NBP.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

    public AbsenceExcuseService(AbsenceExcuseRepository absenceExcuseRepository,
                                AttendanceRepository attendanceRepository,
                                CourseSessionRepository courseSessionRepository,
                                CourseRepository courseRepository,
                                ProfessorRepository professorRepository,
                                StudentRepository studentRepository,
                                NotificationService notificationService) {
        this.absenceExcuseRepository = absenceExcuseRepository;
        this.attendanceRepository = attendanceRepository;
        this.courseSessionRepository = courseSessionRepository;
        this.courseRepository = courseRepository;
        this.professorRepository = professorRepository;
        this.studentRepository = studentRepository;
        this.notificationService = notificationService;
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

    // --- Workflow methods ---

    public AbsenceExcuse submitExcuse(Long courseSessionId, String reason, byte[] document, String documentName, User currentUser) {
        Student student = studentRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a student"));

        // Verify the session actually exists
        courseSessionRepository.findById(courseSessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Course session not found"));

        // Block only if explicitly marked present — no record means absent
        attendanceRepository.findByStudentIdAndCourseSessionId(student.getId(), courseSessionId)
                .ifPresent(attendance -> {
                    if (attendance.isPresent()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Student was present in this session — no excuse needed");
                    }
                });

        if (absenceExcuseRepository.findByStudentIdAndCourseSessionId(student.getId(), courseSessionId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "An excuse for this session has already been submitted");
        }

        AbsenceExcuse excuse = new AbsenceExcuse();
        excuse.setStudentId(student.getId());
        excuse.setCourseSessionId(courseSessionId);
        excuse.setReason(reason);
        excuse.setSubmittedAt(LocalDateTime.now());
        excuse.setStatus("PENDING");
        excuse.setReviewedBy(null);
        excuse.setDocument(document);
        excuse.setDocumentName(documentName);

        Long generatedId = absenceExcuseRepository.saveAndReturnId(excuse);
        excuse.setId(generatedId);
        return excuse;
    }

    public void approveExcuse(Long excuseId, User currentUser) {
        reviewExcuse(excuseId, currentUser, "APPROVED");
    }

    public void rejectExcuse(Long excuseId, User currentUser) {
        reviewExcuse(excuseId, currentUser, "REJECTED");
    }

    private void reviewExcuse(Long excuseId, User currentUser, String newStatus) {
        Professor professor = professorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a professor"));

        AbsenceExcuse excuse = absenceExcuseRepository.findById(excuseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Excuse not found"));

        CourseSession session = courseSessionRepository.findById(excuse.getCourseSessionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course session not found"));

        Course course = courseRepository.findById(session.getCourseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (!course.getProfessorId().equals(professor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You do not own this course session");
        }

        if (!"PENDING".equals(excuse.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Excuse has already been reviewed");
        }

        excuse.setStatus(newStatus);
        excuse.setReviewedBy(currentUser.getId());
        absenceExcuseRepository.update(excuse);

        Student student = studentRepository.findById(excuse.getStudentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        String sessionDate = session.getSessionStartTime() != null
                ? session.getSessionStartTime().toLocalDate().toString()
                : "unknown date";

        boolean approved = "APPROVED".equals(newStatus);
        String title   = approved ? "Excuse Approved" : "Excuse Rejected";
        String action  = approved ? "approved" : "rejected";
        String message = "Excuse " + action + " for " + course.getName() + " on " + sessionDate;
        String type    = approved ? "EXCUSE_APPROVED" : "EXCUSE_REJECTED";

        Notification notification = new Notification();
        notification.setUserId(student.getUserId());
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setNotificationType(type);
        notification.setCourseSessionId(excuse.getCourseSessionId());
        notificationService.save(notification);
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
