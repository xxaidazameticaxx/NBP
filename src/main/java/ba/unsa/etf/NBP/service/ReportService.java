package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.dto.report.CourseAttendanceReportDto;
import ba.unsa.etf.NBP.dto.report.StudentAttendanceReportDto;
import ba.unsa.etf.NBP.repository.ReportRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportService {

    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public List<CourseAttendanceReportDto> getCourseAttendanceReport(Long courseId) {
        return reportRepository.getCourseAttendanceReport(courseId);
    }

    public List<StudentAttendanceReportDto> getStudentAttendanceReport(Long studentId) {
        return reportRepository.getStudentAttendanceReport(studentId);
    }
}