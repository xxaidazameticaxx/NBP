package ba.unsa.etf.NBP.dto.report;

public class CourseAttendanceReportDto {

    private Long studentId;
    private String fullName;
    private String indexNumber;
    private int totalSessions;
    private int attended;
    private int absent;
    private double percentage;

    public CourseAttendanceReportDto() {}

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getIndexNumber() { return indexNumber; }
    public void setIndexNumber(String indexNumber) { this.indexNumber = indexNumber; }

    public int getTotalSessions() { return totalSessions; }
    public void setTotalSessions(int totalSessions) { this.totalSessions = totalSessions; }

    public int getAttended() { return attended; }
    public void setAttended(int attended) { this.attended = attended; }

    public int getAbsent() { return absent; }
    public void setAbsent(int absent) { this.absent = absent; }

    public double getPercentage() { return percentage; }
    public void setPercentage(double percentage) { this.percentage = percentage; }
}