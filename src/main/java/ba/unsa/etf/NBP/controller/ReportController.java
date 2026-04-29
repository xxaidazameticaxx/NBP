package ba.unsa.etf.NBP.controller;

import ba.unsa.etf.NBP.dto.report.CourseAttendanceReportDto;
import ba.unsa.etf.NBP.dto.report.StudentAttendanceReportDto;
import ba.unsa.etf.NBP.model.Report;
import ba.unsa.etf.NBP.service.CourseService;
import ba.unsa.etf.NBP.service.PdfReportService;
import ba.unsa.etf.NBP.service.ReportService;
import ba.unsa.etf.NBP.service.StudentService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Reporting endpoints under {@code /reports} for aggregated attendance data.
 */
@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;
    private final CourseService courseService;
    private final StudentService studentService;
    private final PdfReportService pdfReportService;

    public ReportController(ReportService reportService,
                            CourseService courseService,
                            StudentService studentService,
                            PdfReportService pdfReportService) {
        this.reportService = reportService;
        this.courseService = courseService;
        this.studentService = studentService;
        this.pdfReportService = pdfReportService;
    }

    /**
     * Returns an attendance report for every student in a course.
     *
     * @param courseId course ID
     * @return per-student attendance totals for the given course
     * @throws ResponseStatusException 404 if the course does not exist
     */
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<CourseAttendanceReportDto>> getCourseAttendanceReport(@PathVariable Long courseId) {
        courseService.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        List<CourseAttendanceReportDto> report = reportService.getCourseAttendanceReport(courseId);
        return ResponseEntity.ok(report);
    }

    /**
     * Returns an attendance report for a student across every course they are enrolled in.
     *
     * @param studentId student ID
     * @return per-course attendance totals for the given student
     * @throws ResponseStatusException 404 if the student does not exist
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<StudentAttendanceReportDto>> getStudentAttendanceReport(@PathVariable Long studentId) {
        studentService.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        List<StudentAttendanceReportDto> report = reportService.getStudentAttendanceReport(studentId);
        return ResponseEntity.ok(report);
    }

    /**
     * Downloads a PDF attendance report for a course and saves it to the database.
     *
     * @param courseId course ID
     * @return PDF file as a byte array
     * @throws ResponseStatusException 404 if the course does not exist
     */
    @GetMapping("/course/{courseId}/download")
    public ResponseEntity<byte[]> downloadCourseAttendanceReport(@PathVariable Long courseId) {
        courseService.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        List<CourseAttendanceReportDto> data = reportService.getCourseAttendanceReport(courseId);
        byte[] pdf = pdfReportService.generateCourseAttendanceReport(courseId, data);
        reportService.saveReport(Report.ReportType.COURSE_ATTENDANCE.name(), pdf);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=course_" + courseId + "_attendance.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    /**
     * Downloads a PDF attendance report for a student and saves it to the database.
     *
     * @param studentId student ID
     * @return PDF file as a byte array
     * @throws ResponseStatusException 404 if the student does not exist
     */
    @GetMapping("/student/{studentId}/download")
    public ResponseEntity<byte[]> downloadStudentAttendanceReport(@PathVariable Long studentId) {
        studentService.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        List<StudentAttendanceReportDto> data = reportService.getStudentAttendanceReport(studentId);
        byte[] pdf = pdfReportService.generateStudentAttendanceReport(studentId, data);
        reportService.saveReport(Report.ReportType.STUDENT_ATTENDANCE.name(), pdf);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=student_" + studentId + "_attendance.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
