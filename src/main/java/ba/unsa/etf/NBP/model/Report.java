package ba.unsa.etf.NBP.model;

public class Report {

    private Long id;
    private String type;
    private byte[] content;

    public Report() {}

    public Report(Long id, String type, byte[] content) {
        this.id = id;
        this.type = type;
        this.content = content;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public byte[] getContent() { return content; }
    public void setContent(byte[] content) { this.content = content; }

    public enum ReportType {
        COURSE_ATTENDANCE,
        STUDENT_ATTENDANCE
    }
}
