package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.model.AttendanceAlert;
import ba.unsa.etf.NBP.model.Attendance;
import ba.unsa.etf.NBP.repository.AttendanceAlertRepository;
import ba.unsa.etf.NBP.repository.AttendanceRepository;
import ba.unsa.etf.NBP.repository.CourseRepository;
import ba.unsa.etf.NBP.repository.CourseSessionRepository;
import ba.unsa.etf.NBP.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Detects and manages student attendance trajectory alerts.
 * <p>
 * Analyzes attendance patterns over recent weeks to identify students whose
 * attendance is declining rapidly (dangerous trajectory). Generates alerts
 * for professor review with suggested interventions.
 */
@Service
public class AttendanceAlertService {

    private final AttendanceAlertRepository alertRepository;
    private final AttendanceRepository attendanceRepository;
    private final CourseRepository courseRepository;
    private final CourseSessionRepository courseSessionRepository;
    private final StudentRepository studentRepository;

    // Configuration thresholds
    private static final int WEEKS_TO_ANALYZE = 6;
    private static final double CRITICAL_DECLINE_THRESHOLD = -20.0; // percentage points per week
    private static final double HIGH_DECLINE_THRESHOLD = -12.0;
    private static final double MEDIUM_DECLINE_THRESHOLD = -7.0;
    private static final double CRITICAL_ATTENDANCE_THRESHOLD = 0.40; // 40%
    private static final double HIGH_ATTENDANCE_THRESHOLD = 0.60; // 60%

    public AttendanceAlertService(AttendanceAlertRepository alertRepository,
                                   AttendanceRepository attendanceRepository,
                                   CourseRepository courseRepository,
                                   CourseSessionRepository courseSessionRepository,
                                   StudentRepository studentRepository) {
        this.alertRepository = alertRepository;
        this.attendanceRepository = attendanceRepository;
        this.courseRepository = courseRepository;
        this.courseSessionRepository = courseSessionRepository;
        this.studentRepository = studentRepository;
    }

    /**
     * Analyzes student attendance trajectory and creates alert if dangerous decline detected.
     *
     * @param studentId student ID to analyze
     * @param courseId course ID to check
     * @return Optional containing the created alert, or empty if no decline detected
     */
    public Optional<AttendanceAlert> analyzeAndCreateAlert(Long studentId, Long courseId) {
        if (!studentRepository.findById(studentId).isPresent() ||
            !courseRepository.findById(courseId).isPresent()) {
            return Optional.empty();
        }

        // Check if unresolved alert already exists
        if (alertRepository.existsUnresolvedForStudentAndCourse(studentId, courseId)) {
            return Optional.empty();
        }

        // Check if alert was recently resolved (within grace period of 2 attendance events)
        Optional<AttendanceAlert> recentlyResolved = alertRepository.findMostRecentlyResolvedAlert(studentId, courseId);
        if (recentlyResolved.isPresent()) {
            int eventsSinceResolution = countAttendanceEventsSinceDate(studentId, courseId, recentlyResolved.get().getResolvedAt());
            if (eventsSinceResolution < 2) {
                return Optional.empty(); // Still in grace period
            }
        }

        List<Attendance> attendance = attendanceRepository.findByStudentId(studentId);
        Map<Long, Attendance> attendanceBySession = attendance.stream()
                .collect(Collectors.toMap(Attendance::getCourseSessionId, a -> a));

        List<Attendance> courseAttendance = attendance.stream()
                .filter(a -> isCourseSession(a.getCourseSessionId(), courseId))
                .collect(Collectors.toList());

        if (courseAttendance.size() < 2) {
            return Optional.empty();
        }

        TrajectoryAnalysis analysis = analyzeTrajectory(courseAttendance, WEEKS_TO_ANALYZE);

        if (analysis.isRisky()) {
            AttendanceAlert alert = new AttendanceAlert(
                    studentId,
                    courseId,
                    analysis.getRiskLevel(),
                    analysis.getAttendanceRate(),
                    analysis.getWeeklyTrend(),
                    analysis.getReason(),
                    LocalDateTime.now(),
                    false,
                    analysis.getWeeksAnalyzed()
            );

            Long alertId = alertRepository.save(alert);
            alert.setId(alertId);
            return Optional.of(alert);
        }

        return Optional.empty();
    }

    /**
     * Analyzes all courses for a student and returns all risky patterns.
     *
     * @param studentId student ID to analyze
     * @return list of created alerts
     */
    public List<AttendanceAlert> analyzeAllCoursesForStudent(Long studentId) {
        List<AttendanceAlert> alerts = new ArrayList<>();
        List<Attendance> allAttendance = attendanceRepository.findByStudentId(studentId);

        if (allAttendance.isEmpty()) {
            return alerts;
        }

        // Group by course
        Map<Long, List<Attendance>> byCourse = allAttendance.stream()
                .collect(Collectors.groupingBy(a -> getCourseIdFromSession(a.getCourseSessionId())));

        for (Long courseId : byCourse.keySet()) {
            analyzeAndCreateAlert(studentId, courseId).ifPresent(alerts::add);
        }

        return alerts;
    }

    /**
     * Gets all active (unresolved) alerts for a course.
     *
     * @param courseId course ID
     * @return list of unresolved alerts
     */
    public List<AttendanceAlert> getAlertsForCourse(Long courseId) {
        return alertRepository.findByCourseId(courseId);
    }

    /**
     * Gets all active (unresolved) alerts for a student.
     *
     * @param studentId student ID
     * @return list of unresolved alerts
     */
    public List<AttendanceAlert> getAlertsForStudent(Long studentId) {
        return alertRepository.findByStudentId(studentId);
    }

    /**
     * Mark an alert as resolved with an action taken.
     *
     * @param alertId alert ID
     * @param actionTaken description of action taken (e.g., "Email sent to student")
     */
    public void resolveAlert(Long alertId, String actionTaken) {
        alertRepository.markResolved(alertId, actionTaken);
    }

    /**
     * Gets details of a specific alert.
     *
     * @param alertId alert ID
     * @return the alert, or empty if not found
     */
    public Optional<AttendanceAlert> getAlertDetails(Long alertId) {
        return alertRepository.findById(alertId);
    }

    /**
     * Analyzes attendance trajectory using linear regression on weekly attendance rates.
     * Returns analysis including trend line slope (trend per week) and current attendance rate.
     */
    private TrajectoryAnalysis analyzeTrajectory(List<Attendance> attendanceList, int weeksToAnalyze) {
        if (attendanceList.size() < 2) {
            return new TrajectoryAnalysis(0, 0.0, "Insufficient data", false, 0);
        }

        // Sort by marked date
        attendanceList.sort((a, b) -> {
            if (a.getMarkedAt() == null) return 1;
            if (b.getMarkedAt() == null) return -1;
            return a.getMarkedAt().compareTo(b.getMarkedAt());
        });

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusWeeks(weeksToAnalyze);

        // Group attendance by week
        Map<Integer, List<Attendance>> byWeek = attendanceList.stream()
                .filter(a -> a.getMarkedAt() != null && a.getMarkedAt().isAfter(cutoff))
                .collect(Collectors.groupingBy(a -> getWeekNumber(a.getMarkedAt())));

        if (byWeek.size() < 2) {
            return new TrajectoryAnalysis(0, 0.0, "Insufficient weeks of data", false, byWeek.size());
        }

        // Calculate weekly attendance rates
        List<Double> weeklyRates = new ArrayList<>();
        byWeek.values().forEach(weekAttendance -> {
            double presentCount = weekAttendance.stream().filter(Attendance::isPresent).count();
            double rate = presentCount / weekAttendance.size() * 100;
            weeklyRates.add(rate);
        });

        if (weeklyRates.size() < 2) {
            return new TrajectoryAnalysis(0, 0.0, "Insufficient data", false, byWeek.size());
        }

        double trend = calculateLinearTrend(weeklyRates);
        double currentRate = weeklyRates.get(weeklyRates.size() - 1);

        String riskLevel = determineRiskLevel(trend, currentRate);
        String reason = buildReason(trend, currentRate, weeklyRates);
        boolean isRisky = !riskLevel.equals("NONE");

        return new TrajectoryAnalysis(trend, currentRate, reason, isRisky, byWeek.size())
                .setRiskLevel(riskLevel);
    }

    private double calculateLinearTrend(List<Double> weeklyRates) {
        if (weeklyRates.size() < 2) return 0.0;

        // Simple linear regression: fit line y = a + bx where x is week number
        int n = weeklyRates.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            double x = i;
            double y = weeklyRates.get(i);
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        return Math.round(slope * 100.0) / 100.0; // Round to 2 decimals
    }

    private String determineRiskLevel(double trend, double currentRate) {
        if (trend <= CRITICAL_DECLINE_THRESHOLD || currentRate < CRITICAL_ATTENDANCE_THRESHOLD) {
            return "CRITICAL";
        }
        if (trend <= HIGH_DECLINE_THRESHOLD || currentRate < HIGH_ATTENDANCE_THRESHOLD) {
            return "HIGH";
        }
        if (trend <= MEDIUM_DECLINE_THRESHOLD) {
            return "MEDIUM";
        }
        return "NONE";
    }

    private String buildReason(double trend, double currentRate, List<Double> weeklyRates) {
        StringBuilder reason = new StringBuilder();

        // Trend description
        if (trend < 0) {
            reason.append(String.format("Attendance declining at %.1f%% per week. ", Math.abs(trend)));
        } else {
            reason.append(String.format("Attendance improving at %.1f%% per week. ", trend));
        }

        // Current rate context
        reason.append(String.format("Current attendance: %.0f%%. ", currentRate));

        // Trajectory description
        if (weeklyRates.size() >= 2) {
            double firstRate = weeklyRates.get(0);
            double lastRate = weeklyRates.get(weeklyRates.size() - 1);
            double totalChange = lastRate - firstRate;
            reason.append(String.format("Changed from %.0f%% to %.0f%% (%.0f%% total change).",
                    firstRate, lastRate, totalChange));
        }

        return reason.toString();
    }

    private int getWeekNumber(LocalDateTime date) {
        return date.get(java.time.temporal.WeekFields.ISO.weekOfYear());
    }

    private boolean isCourseSession(Long sessionId, Long courseId) {
        return courseSessionRepository.findById(sessionId)
                .map(cs -> cs.getCourseId().equals(courseId))
                .orElse(false);
    }

    private Long getCourseIdFromSession(Long sessionId) {
        return courseSessionRepository.findById(sessionId)
                .map(cs -> cs.getCourseId())
                .orElse(null);
    }

    private int countAttendanceEventsSinceDate(Long studentId, Long courseId, LocalDateTime since) {
        List<Attendance> attendance = attendanceRepository.findByStudentId(studentId);
        return (int) attendance.stream()
                .filter(a -> isCourseSession(a.getCourseSessionId(), courseId))
                .filter(a -> a.getMarkedAt() != null && a.getMarkedAt().isAfter(since))
                .count();
    }


    // Inner class for analysis results
    public static class TrajectoryAnalysis {
        private final double weeklyTrend;
        private final double attendanceRate;
        private final String reason;
        private final boolean risky;
        private final int weeksAnalyzed;
        private String riskLevel;

        public TrajectoryAnalysis(double weeklyTrend, double attendanceRate, String reason, boolean risky, int weeksAnalyzed) {
            this.weeklyTrend = weeklyTrend;
            this.attendanceRate = attendanceRate;
            this.reason = reason;
            this.risky = risky;
            this.weeksAnalyzed = weeksAnalyzed;
        }

        public TrajectoryAnalysis setRiskLevel(String riskLevel) {
            this.riskLevel = riskLevel;
            return this;
        }

        public double getWeeklyTrend() { return weeklyTrend; }
        public double getAttendanceRate() { return attendanceRate; }
        public String getReason() { return reason; }
        public boolean isRisky() { return risky; }
        public int getWeeksAnalyzed() { return weeksAnalyzed; }
        public String getRiskLevel() { return riskLevel; }
    }
}
