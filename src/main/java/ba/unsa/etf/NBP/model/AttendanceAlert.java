package ba.unsa.etf.NBP.model;

import java.time.LocalDateTime;

public class AttendanceAlert {

    private Long id;
    private Long studentId;
    private Long courseId;
    private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL
    private Double attendanceRate;
    private Double weeklyTrend; // percentage points per week (negative = declining)
    private String alertReason;
    private LocalDateTime createdAt;
    private Boolean isResolved;
    private String actionTaken;
    private LocalDateTime resolvedAt;
    private Integer weeksAnalyzed;

    public AttendanceAlert() {}

    public AttendanceAlert(Long studentId, Long courseId, String riskLevel, Double attendanceRate,
                          Double weeklyTrend, String alertReason, LocalDateTime createdAt,
                          Boolean isResolved, Integer weeksAnalyzed) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.riskLevel = riskLevel;
        this.attendanceRate = attendanceRate;
        this.weeklyTrend = weeklyTrend;
        this.alertReason = alertReason;
        this.createdAt = createdAt;
        this.isResolved = isResolved;
        this.weeksAnalyzed = weeksAnalyzed;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }

    public Double getAttendanceRate() { return attendanceRate; }
    public void setAttendanceRate(Double attendanceRate) { this.attendanceRate = attendanceRate; }

    public Double getWeeklyTrend() { return weeklyTrend; }
    public void setWeeklyTrend(Double weeklyTrend) { this.weeklyTrend = weeklyTrend; }

    public String getAlertReason() { return alertReason; }
    public void setAlertReason(String alertReason) { this.alertReason = alertReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Boolean isResolved() { return isResolved; }
    public void setResolved(Boolean resolved) { isResolved = resolved; }

    public String getActionTaken() { return actionTaken; }
    public void setActionTaken(String actionTaken) { this.actionTaken = actionTaken; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public Integer getWeeksAnalyzed() { return weeksAnalyzed; }
    public void setWeeksAnalyzed(Integer weeksAnalyzed) { this.weeksAnalyzed = weeksAnalyzed; }
}
