package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.dto.report.CourseAttendanceReportDto;
import ba.unsa.etf.NBP.dto.report.StudentAttendanceReportDto;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Generates PDF reports for attendance data using OpenPDF.
 */
@Service
public class PdfReportService {

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font CELL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10);

    /**
     * Generates a PDF report with per-student attendance data for a course.
     *
     * @param courseId course ID used in the report title
     * @param data     list of attendance records per student
     * @return PDF content as a byte array
     */
    public byte[] generateCourseAttendanceReport(Long courseId, List<CourseAttendanceReportDto> data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, out);
        document.open();

        document.add(new Paragraph("Course Attendance Report — Course ID: " + courseId, TITLE_FONT));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 2, 1.5f, 1.5f, 1.5f, 1.5f});

        addHeaderCell(table, "Student");
        addHeaderCell(table, "Index");
        addHeaderCell(table, "Total");
        addHeaderCell(table, "Attended");
        addHeaderCell(table, "Absent");
        addHeaderCell(table, "Percentage");

        for (CourseAttendanceReportDto row : data) {
            table.addCell(new Phrase(row.getFullName(), CELL_FONT));
            table.addCell(new Phrase(row.getIndexNumber(), CELL_FONT));
            table.addCell(new Phrase(String.valueOf(row.getTotalSessions()), CELL_FONT));
            table.addCell(new Phrase(String.valueOf(row.getAttended()), CELL_FONT));
            table.addCell(new Phrase(String.valueOf(row.getAbsent()), CELL_FONT));
            table.addCell(new Phrase(row.getPercentage() + "%", CELL_FONT));
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }

    /**
     * Generates a PDF report with per-course attendance data for a student.
     *
     * @param studentId student ID used in the report title
     * @param data      list of attendance records per course
     * @return PDF content as a byte array
     */
    public byte[] generateStudentAttendanceReport(Long studentId, List<StudentAttendanceReportDto> data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, out);
        document.open();

        document.add(new Paragraph("Student Attendance Report — Student ID: " + studentId, TITLE_FONT));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 1.5f, 1.5f, 1.5f});

        addHeaderCell(table, "Course");
        addHeaderCell(table, "Total");
        addHeaderCell(table, "Attended");
        addHeaderCell(table, "Percentage");

        for (StudentAttendanceReportDto row : data) {
            table.addCell(new Phrase(row.getCourseName(), CELL_FONT));
            table.addCell(new Phrase(String.valueOf(row.getTotalSessions()), CELL_FONT));
            table.addCell(new Phrase(String.valueOf(row.getAttended()), CELL_FONT));
            table.addCell(new Phrase(row.getPercentage() + "%", CELL_FONT));
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setBackgroundColor(new Color(220, 220, 220));
        cell.setPadding(5);
        table.addCell(cell);
    }
}
