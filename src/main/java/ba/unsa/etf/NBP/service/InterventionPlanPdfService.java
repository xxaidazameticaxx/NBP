package ba.unsa.etf.NBP.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class InterventionPlanPdfService {

    public byte[] generateInterventionPlanPdf(Long courseId, CourseInterventionService.InterventionPlan plan) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            // Title
            Paragraph title = new Paragraph("Course Intervention Plan",
                    new Font(Font.HELVETICA, 20, Font.BOLD));
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            // Date
            Paragraph date = new Paragraph("Generated: " + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    new Font(Font.HELVETICA, 10));
            date.setAlignment(Element.ALIGN_CENTER);
            document.add(date);
            document.add(new Paragraph(" "));

            // Overall Assessment
            Paragraph assessmentTitle = new Paragraph("Course Assessment",
                    new Font(Font.HELVETICA, 14, Font.BOLD));
            assessmentTitle.setSpacingBefore(10);
            document.add(assessmentTitle);

            Paragraph assessmentText = new Paragraph(plan.overallAssessment,
                    new Font(Font.HELVETICA, 11));
            assessmentText.setSpacingAfter(15);
            document.add(assessmentText);

            // Per-Student Recommendations
            Paragraph recommendationsTitle = new Paragraph("Student Recommendations",
                    new Font(Font.HELVETICA, 14, Font.BOLD));
            recommendationsTitle.setSpacingBefore(10);
            document.add(recommendationsTitle);

            if (plan.studentRecommendations != null && !plan.studentRecommendations.isEmpty()) {
                for (CourseInterventionService.StudentInterventionRecommendation rec : plan.studentRecommendations) {
                    // Student name/ID
                    Paragraph studentHeader = new Paragraph(
                            "Student ID: " + rec.studentId,
                            new Font(Font.HELVETICA, 11, Font.BOLD));
                    studentHeader.setSpacingBefore(10);
                    document.add(studentHeader);

                    // Risk level
                    Font riskFont = new Font(Font.HELVETICA, 11);
                    riskFont.setColor(getColorForRiskLevel(rec.riskLevel));
                    Paragraph riskLevel = new Paragraph("Risk Level: " + rec.riskLevel, riskFont);
                    document.add(riskLevel);

                    // Metrics
                    Paragraph metrics = new Paragraph(
                            "Attendance: " + String.format("%.1f%%", rec.attendanceRate) +
                            " | Weekly Decline: " + String.format("%.1f%%", rec.weeklyDecline),
                            new Font(Font.HELVETICA, 10));
                    document.add(metrics);

                    // Recommendation text
                    Paragraph recommendation = new Paragraph(rec.recommendation,
                            new Font(Font.HELVETICA, 10));
                    recommendation.setSpacingAfter(10);
                    document.add(recommendation);
                }
            }

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate intervention plan PDF", e);
        }
    }

    private java.awt.Color getColorForRiskLevel(String riskLevel) {
        switch (riskLevel) {
            case "CRITICAL":
                return Color.RED;
            case "HIGH":
                return new Color(255, 165, 0); // Orange
            case "MEDIUM":
                return new Color(255, 192, 0); // Yellow
            default:
                return Color.GREEN;
        }
    }
}
