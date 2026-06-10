package ba.unsa.etf.NBP.controller;

import ba.unsa.etf.NBP.model.AttendanceAlert;
import ba.unsa.etf.NBP.model.Course;
import ba.unsa.etf.NBP.model.Student;
import ba.unsa.etf.NBP.service.AttendanceAlertService;
import ba.unsa.etf.NBP.service.CourseInterventionService;
import ba.unsa.etf.NBP.service.CourseService;
import ba.unsa.etf.NBP.service.InterventionPlanPdfService;
import ba.unsa.etf.NBP.service.StudentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("AttendanceAlertController - Service Integration Tests")
class AttendanceAlertControllerTest {

    @Mock
    private AttendanceAlertService alertService;

    @Mock
    private StudentService studentService;

    @Mock
    private CourseService courseService;

    @Mock
    private CourseInterventionService interventionService;

    @Mock
    private InterventionPlanPdfService pdfService;

    private AttendanceAlertController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new AttendanceAlertController(alertService, studentService, courseService, interventionService, pdfService);
    }

    @Test
    @DisplayName("getAlertsForCourse - returns course alerts")
    void testGetAlertsForCourse_Success() {
        Long courseId = 1L;
        List<AttendanceAlert> mockAlerts = List.of(
                createMockAlert(1L, 1L, courseId),
                createMockAlert(2L, 2L, courseId)
        );

        when(courseService.findById(courseId)).thenReturn(Optional.of(new Course()));
        when(alertService.getAlertsForCourse(courseId)).thenReturn(mockAlerts);

        var result = controller.getAlertsForCourse(courseId);

        assertNotNull(result);
        assertEquals(2, result.getBody().size());
    }

    @Test
    @DisplayName("getAlertsForStudent - returns student alerts")
    void testGetAlertsForStudent_Success() {
        Long studentId = 1L;
        List<AttendanceAlert> mockAlerts = List.of(
                createMockAlert(1L, studentId, 1L),
                createMockAlert(2L, studentId, 2L)
        );

        when(studentService.findById(studentId)).thenReturn(Optional.of(new Student()));
        when(alertService.getAlertsForStudent(studentId)).thenReturn(mockAlerts);

        var result = controller.getAlertsForStudent(studentId);

        assertNotNull(result);
        assertEquals(2, result.getBody().size());
    }

    @Test
    @DisplayName("getAlert - returns alert details")
    void testGetAlert_Success() {
        Long alertId = 1L;
        AttendanceAlert mockAlert = createMockAlert(alertId, 1L, 1L);

        when(alertService.getAlertDetails(alertId)).thenReturn(Optional.of(mockAlert));

        var result = controller.getAlert(alertId);

        assertNotNull(result);
        assertEquals(alertId, result.getBody().getId());
    }

    @Test
    @DisplayName("resolveAlert - marks alert as resolved")
    void testResolveAlert_Success() {
        Long alertId = 1L;
        String actionTaken = "Email sent to student";
        AttendanceAlert mockAlert = createMockAlert(alertId, 1L, 1L);

        when(alertService.getAlertDetails(alertId)).thenReturn(Optional.of(mockAlert));
        doNothing().when(alertService).resolveAlert(alertId, actionTaken);

        var result = controller.resolveAlert(alertId, actionTaken);

        assertNotNull(result);
        verify(alertService).resolveAlert(alertId, actionTaken);
    }

    // Helper method
    private AttendanceAlert createMockAlert(Long id, Long studentId, Long courseId) {
        AttendanceAlert alert = new AttendanceAlert();
        alert.setId(id);
        alert.setStudentId(studentId);
        alert.setCourseId(courseId);
        alert.setRiskLevel("CRITICAL");
        alert.setAttendanceRate(35.0);
        alert.setWeeklyTrend(-25.0);
        alert.setAlertReason("Attendance declining at 25.0% per week");
        alert.setCreatedAt(LocalDateTime.now());
        alert.setResolved(false);
        alert.setWeeksAnalyzed(4);
        return alert;
    }
}
