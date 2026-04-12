package ba.unsa.etf.NBP.dto.report;

public class StudentAttendanceReportDto {

    private Long courseId;
    private String courseName;
    private int totalSessions;
    private int attended;
    private double percentage;

    public StudentAttendanceReportDto() {}

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public int getTotalSessions() { return totalSessions; }
    public void setTotalSessions(int totalSessions) { this.totalSessions = totalSessions; }

    public int getAttended() { return attended; }
    public void setAttended(int attended) { this.attended = attended; }

    public double getPercentage() { return percentage; }
    public void setPercentage(double percentage) { this.percentage = percentage; }
}