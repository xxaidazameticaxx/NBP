package ba.unsa.etf.NBP.controller;

import ba.unsa.etf.NBP.model.AttendanceAlert;
import ba.unsa.etf.NBP.service.AttendanceAlertService;
import ba.unsa.etf.NBP.service.CourseInterventionService;
import ba.unsa.etf.NBP.service.CourseService;
import ba.unsa.etf.NBP.service.InterventionPlanPdfService;
import ba.unsa.etf.NBP.service.StudentService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Endpoints under {@code /alerts} for student attendance trajectory monitoring.
 * Detects and manages alerts for rapid attendance decline.
 */
@RestController
@RequestMapping("/alerts")
public class AttendanceAlertController {

    private final AttendanceAlertService alertService;
    private final StudentService studentService;
    private final CourseService courseService;
    private final CourseInterventionService interventionService;
    private final InterventionPlanPdfService pdfService;

    public AttendanceAlertController(AttendanceAlertService alertService,
                                     StudentService studentService,
                                     CourseService courseService,
                                     CourseInterventionService interventionService,
                                     InterventionPlanPdfService pdfService) {
        this.alertService = alertService;
        this.studentService = studentService;
        this.courseService = courseService;
        this.interventionService = interventionService;
        this.pdfService = pdfService;
    }

    /**
     * Gets all active alerts for a course.
     *
     * @param courseId course ID
     * @return list of unresolved alerts for the course
     */
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<AttendanceAlert>> getAlertsForCourse(@PathVariable Long courseId) {
        courseService.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        List<AttendanceAlert> alerts = alertService.getAlertsForCourse(courseId);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Gets all active alerts for a student.
     *
     * @param studentId student ID
     * @return list of unresolved alerts for the student
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<AttendanceAlert>> getAlertsForStudent(@PathVariable Long studentId) {
        studentService.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        List<AttendanceAlert> alerts = alertService.getAlertsForStudent(studentId);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Gets details of a specific alert.
     *
     * @param alertId alert ID
     * @return the alert details
     */
    @GetMapping("/{alertId}")
    public ResponseEntity<AttendanceAlert> getAlert(@PathVariable Long alertId) {
        return alertService.getAlertDetails(alertId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Marks an alert as resolved (e.g., after professor takes action).
     *
     * @param alertId alert ID
     * @param actionTaken description of action taken
     * @return 200 OK
     */
    @PostMapping("/{alertId}/resolve")
    public ResponseEntity<?> resolveAlert(@PathVariable Long alertId,
                                          @RequestParam String actionTaken) {
        alertService.getAlertDetails(alertId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found"));

        alertService.resolveAlert(alertId, actionTaken);
        return ResponseEntity.ok().build();
    }

    /**
     * Generates a comprehensive intervention plan PDF for a course with smart recommendations.
     * Analyzes attendance patterns and provides rule-based recommendations for each student.
     *
     * @param courseId course ID
     * @return intervention plan as PDF file
     */
    @GetMapping("/course/{courseId}/intervention-plan")
    public ResponseEntity<byte[]> getInterventionPlan(@PathVariable Long courseId) {
        courseService.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        CourseInterventionService.InterventionPlan plan = interventionService.generateInterventionPlan(courseId);
        byte[] pdf = pdfService.generateInterventionPlanPdf(courseId, plan);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=intervention_plan_" + courseId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

}
