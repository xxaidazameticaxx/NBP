package ba.unsa.etf.NBP.controller;

import ba.unsa.etf.NBP.dto.report.CourseAttendanceReportDto;
import ba.unsa.etf.NBP.dto.report.StudentAttendanceReportDto;
import ba.unsa.etf.NBP.service.AuthService;
import ba.unsa.etf.NBP.service.CourseService;
import ba.unsa.etf.NBP.service.ReportService;
import ba.unsa.etf.NBP.service.StudentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;
    private final CourseService courseService;
    private final StudentService studentService;

    public ReportController(ReportService reportService,
                            CourseService courseService,
                            StudentService studentService) {
        this.reportService = reportService;
        this.courseService = courseService;
        this.studentService = studentService;
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<CourseAttendanceReportDto>> getCourseAttendanceReport(@PathVariable Long courseId) {
        courseService.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        List<CourseAttendanceReportDto> report = reportService.getCourseAttendanceReport(courseId);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<StudentAttendanceReportDto>> getStudentAttendanceReport(@PathVariable Long studentId) {
        studentService.findById(studentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));

        List<StudentAttendanceReportDto> report = reportService.getStudentAttendanceReport(studentId);
        return ResponseEntity.ok(report);
    }
}