package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.model.Attendance;
import ba.unsa.etf.NBP.repository.AttendanceRepository;
import ba.unsa.etf.NBP.repository.CourseSessionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CourseInterventionService {

    private final AttendanceRepository attendanceRepository;
    private final CourseSessionRepository courseSessionRepository;

    public CourseInterventionService(AttendanceRepository attendanceRepository,
                                     CourseSessionRepository courseSessionRepository) {
        this.attendanceRepository = attendanceRepository;
        this.courseSessionRepository = courseSessionRepository;
    }

    public InterventionPlan generateInterventionPlan(Long courseId) {
        List<Attendance> courseAttendance = attendanceRepository.findByCourseId(courseId);

        if (courseAttendance.isEmpty()) {
            return new InterventionPlan(courseId, "Nema podataka o pohađanju", new ArrayList<>());
        }

        // Calculate sessions remaining
        List<Attendance> allSessions = courseSessionRepository.findAll().stream()
                .filter(session -> session.getCourseId().equals(courseId))
                .flatMap(session -> courseAttendance.stream()
                        .filter(a -> a.getCourseSessionId().equals(session.getId())))
                .collect(Collectors.toList());

        int totalSessions = (int) courseSessionRepository.findAll().stream()
                .filter(s -> s.getCourseId().equals(courseId))
                .count();
        int completedSessions = (int) courseAttendance.stream()
                .map(Attendance::getCourseSessionId)
                .distinct()
                .count();
        int sessionsRemaining = totalSessions - completedSessions;

        // Group by student and generate recommendations
        Map<Long, StudentAttendanceMetrics> studentMetrics = calculateStudentMetrics(courseAttendance);
        List<StudentInterventionRecommendation> recommendations = new ArrayList<>();

        for (Map.Entry<Long, StudentAttendanceMetrics> entry : studentMetrics.entrySet()) {
            StudentAttendanceMetrics metrics = entry.getValue();
            String recommendation = generateRecommendation(
                    entry.getKey(),
                    metrics,
                    sessionsRemaining
            );
            recommendations.add(new StudentInterventionRecommendation(
                    entry.getKey(),
                    metrics.attendanceRate,
                    metrics.weeklyDecline,
                    metrics.riskLevel,
                    recommendation
            ));
        }

        // Sort by risk level
        recommendations.sort((a, b) -> {
            Integer riskOrder1 = getRiskOrder(a.riskLevel);
            Integer riskOrder2 = getRiskOrder(b.riskLevel);
            return riskOrder1.compareTo(riskOrder2);
        });

        String overallAssessment = generateOverallAssessment(recommendations, sessionsRemaining, totalSessions);

        return new InterventionPlan(courseId, overallAssessment, recommendations);
    }

    private Map<Long, StudentAttendanceMetrics> calculateStudentMetrics(List<Attendance> attendance) {
        Map<Long, List<Attendance>> byStudent = attendance.stream()
                .collect(Collectors.groupingBy(Attendance::getStudentId));

        Map<Long, StudentAttendanceMetrics> metrics = new HashMap<>();

        for (Map.Entry<Long, List<Attendance>> entry : byStudent.entrySet()) {
            List<Attendance> studentAttendance = entry.getValue();

            if (studentAttendance.size() < 2) {
                continue;
            }

            long presentCount = studentAttendance.stream().filter(Attendance::isPresent).count();
            double attendanceRate = (presentCount / (double) studentAttendance.size()) * 100;

            // Calculate weekly decline trend
            List<Boolean> attendanceList = studentAttendance.stream()
                    .map(Attendance::isPresent)
                    .collect(Collectors.toList());

            double weeklyDecline = calculateWeeklyDecline(attendanceList);
            String riskLevel = determineRiskLevel(attendanceRate, weeklyDecline);

            metrics.put(entry.getKey(), new StudentAttendanceMetrics(
                    attendanceRate,
                    weeklyDecline,
                    riskLevel,
                    studentAttendance.size()
            ));
        }

        return metrics;
    }

    private double calculateWeeklyDecline(List<Boolean> attendance) {
        if (attendance.size() < 2) return 0;

        // Simplified: last 2 weeks comparison
        int mid = attendance.size() / 2;
        double firstHalf = attendance.subList(0, mid).stream()
                .filter(a -> a)
                .count() / (double) mid * 100;
        double secondHalf = attendance.subList(mid, attendance.size()).stream()
                .filter(a -> a)
                .count() / (double) (attendance.size() - mid) * 100;

        return secondHalf - firstHalf;
    }

    private String determineRiskLevel(double attendanceRate, double weeklyDecline) {
        if (attendanceRate < 40 || weeklyDecline < -20) {
            return "CRITICAL";
        } else if (attendanceRate < 60 || weeklyDecline < -12) {
            return "HIGH";
        } else if (attendanceRate < 80 || weeklyDecline < -7) {
            return "MEDIUM";
        }
        return "NONE";
    }

    private String generateRecommendation(Long studentId, StudentAttendanceMetrics metrics, int sessionsRemaining) {
        StringBuilder rec = new StringBuilder();

        if ("CRITICAL".equals(metrics.riskLevel)) {
            if (sessionsRemaining <= 3) {
                rec.append("🔴 HITNA AKCIJA POTREBNA! Student je u kritičnom statusu sa ")
                        .append(String.format("%.1f", metrics.attendanceRate))
                        .append("% pohađanja. Kurs se završava za samo ")
                        .append(sessionsRemaining)
                        .append(" sesija. ");
                rec.append("Preporuka: ODMAH kontaktirati studenta, razgovor o problemima, ");
                rec.append("razmotriti mogućnost dodatnih testova/testiranja kao kompenzaciju.");
            } else {
                rec.append("🔴 KRITIČNO! Pohađanje je samo ")
                        .append(String.format("%.1f", metrics.attendanceRate))
                        .append("%. Trend pada: ")
                        .append(String.format("%.1f", metrics.weeklyDecline))
                        .append("% po sedmici. ");
                rec.append("Preporuka: Hitna intervencija, tutorski časovi, ");
                rec.append("provjera da li ima zdravstvenih ili ličnih problema.");
            }
        } else if ("HIGH".equals(metrics.riskLevel)) {
            rec.append("🟠 VISOK RIZIK. Pohađanje: ")
                    .append(String.format("%.1f", metrics.attendanceRate))
                    .append("%. Trend: ")
                    .append(String.format("%.1f", metrics.weeklyDecline))
                    .append("% po sedmici. ");

            if (sessionsRemaining <= 5) {
                rec.append("Kurs se završava brzo!");
            } else {
                rec.append("Ima vremena za poboljšanje.");
            }

            rec.append(" Preporuka: Motivacijski razgovor, dodatne vježbe, ");
            rec.append("jasna komunikacija o konsekvencijama.");
        } else if ("MEDIUM".equals(metrics.riskLevel)) {
            rec.append("🟡 SREDNJI RIZIK. Pohađanje: ")
                    .append(String.format("%.1f", metrics.attendanceRate))
                    .append("%. Preporuka: Redovni monitoring, ");
            rec.append("ohrabriti studenta da nastavi sa boljim pohađanjem.");
        } else {
            rec.append("✅ OK - Student ima zadovoljavajuće pohađanje. Nastavi tako!");
        }

        return rec.toString();
    }

    private String generateOverallAssessment(List<StudentInterventionRecommendation> recommendations,
                                            int sessionsRemaining, int totalSessions) {
        long criticalCount = recommendations.stream()
                .filter(r -> "CRITICAL".equals(r.riskLevel))
                .count();
        long highCount = recommendations.stream()
                .filter(r -> "HIGH".equals(r.riskLevel))
                .count();
        long mediumCount = recommendations.stream()
                .filter(r -> "MEDIUM".equals(r.riskLevel))
                .count();

        StringBuilder assessment = new StringBuilder();
        assessment.append("📊 KURS ANALIZA\n\n");
        assessment.append("Sesije: ").append(totalSessions - sessionsRemaining).append("/").append(totalSessions);
        assessment.append(" (").append(sessionsRemaining).append(" preostaje)\n\n");

        assessment.append("Studenti u riziku:\n");
        assessment.append("  🔴 Kritični: ").append(criticalCount).append("\n");
        assessment.append("  🟠 Visoki: ").append(highCount).append("\n");
        assessment.append("  🟡 Srednji: ").append(mediumCount).append("\n\n");

        if (sessionsRemaining <= 3) {
            assessment.append("⚠️ ZAVRŠNI STADIJ: Kurs se brzo završava! ");
            assessment.append("Fokusiraj se na kritične studente - nema vremena za oporavak.\n\n");
        } else if (sessionsRemaining <= 7) {
            assessment.append("⏰ POLA VREMENA: Ima još vremena ali se brzo približava kraj. ");
            assessment.append("Hitno kontaktirati riziče studente.\n\n");
        } else {
            assessment.append("✅ DOVOLJNO VREMENA: Ima još mnogo sesija za intervenciju.\n\n");
        }

        assessment.append("Preporuke:\n");
        if (criticalCount > 0) {
            assessment.append("1. ODMAH: Razgovori sa kritičnim studentima\n");
        }
        if (highCount > 0) {
            assessment.append("2. BRZO: Tutorski časovi za visoko rizične studente\n");
        }
        if (mediumCount > 0) {
            assessment.append("3. MONITORING: Praćenje studenata sa srednjim rizikom\n");
        }
        assessment.append("4. DOKUMENTACIJA: Svi razgovori i akcije trebali bi biti dokumentirani");

        return assessment.toString();
    }

    private Integer getRiskOrder(String riskLevel) {
        return switch (riskLevel) {
            case "CRITICAL" -> 0;
            case "HIGH" -> 1;
            case "MEDIUM" -> 2;
            default -> 3;
        };
    }

    // DTOs
    public static class InterventionPlan {
        public final Long courseId;
        public final String overallAssessment;
        public final List<StudentInterventionRecommendation> studentRecommendations;

        public InterventionPlan(Long courseId, String overallAssessment,
                               List<StudentInterventionRecommendation> studentRecommendations) {
            this.courseId = courseId;
            this.overallAssessment = overallAssessment;
            this.studentRecommendations = studentRecommendations;
        }
    }

    public static class StudentInterventionRecommendation {
        public final Long studentId;
        public final double attendanceRate;
        public final double weeklyDecline;
        public final String riskLevel;
        public final String recommendation;

        public StudentInterventionRecommendation(Long studentId, double attendanceRate,
                                                 double weeklyDecline, String riskLevel,
                                                 String recommendation) {
            this.studentId = studentId;
            this.attendanceRate = attendanceRate;
            this.weeklyDecline = weeklyDecline;
            this.riskLevel = riskLevel;
            this.recommendation = recommendation;
        }
    }

    private static class StudentAttendanceMetrics {
        final double attendanceRate;
        final double weeklyDecline;
        final String riskLevel;
        final int totalSessions;

        StudentAttendanceMetrics(double attendanceRate, double weeklyDecline,
                               String riskLevel, int totalSessions) {
            this.attendanceRate = attendanceRate;
            this.weeklyDecline = weeklyDecline;
            this.riskLevel = riskLevel;
            this.totalSessions = totalSessions;
        }
    }
}
