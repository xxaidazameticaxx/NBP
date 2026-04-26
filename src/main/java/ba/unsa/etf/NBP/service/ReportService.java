package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.dto.report.CourseAttendanceReportDto;
import ba.unsa.etf.NBP.dto.report.StudentAttendanceReportDto;
import ba.unsa.etf.NBP.model.Report;
import ba.unsa.etf.NBP.repository.ReportRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Attendance reporting and analytics.
 * <p>
 * Generates summaries for professors to review course attendance and for
 * students to view their attendance history.
 */
@Service
public class ReportService {

    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    /**
     * Generates attendance statistics for a course.
     *
     * @param courseId course ID
     * @return attendance report for that course
     */
    public List<CourseAttendanceReportDto> getCourseAttendanceReport(Long courseId) {
        return reportRepository.getCourseAttendanceReport(courseId);
    }

    /**
     * Generates attendance history for a student.
     *
     * @param studentId student ID
     * @return attendance report for that student
     */
    public List<StudentAttendanceReportDto> getStudentAttendanceReport(Long studentId) {
        return reportRepository.getStudentAttendanceReport(studentId);
    }

    /**
     * Persists a generated report to the database.
     *
     * @param type    report type identifier
     * @param content report file content as a byte array
     */
    public void saveReport(String type, byte[] content) {
        Report report = new Report();
        report.setType(type);
        report.setContent(content);
        reportRepository.save(report);
    }
}