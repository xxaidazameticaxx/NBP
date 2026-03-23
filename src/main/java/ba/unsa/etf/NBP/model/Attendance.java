package ba.unsa.etf.NBP.model;

import java.time.LocalDateTime;

public class Attendance {

    private Long id;
    private Long studentId;
    private Long courseSessionId;
    private boolean isPresent;
    private LocalDateTime markedAt;
    private String notes;

    public Attendance() {}

    public Attendance(Long id, Long studentId, Long courseSessionId, boolean isPresent,
                      LocalDateTime markedAt, String notes) {
        this.id = id;
        this.studentId = studentId;
        this.courseSessionId = courseSessionId;
        this.isPresent = isPresent;
        this.markedAt = markedAt;
        this.notes = notes;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public Long getCourseSessionId() { return courseSessionId; }
    public void setCourseSessionId(Long courseSessionId) { this.courseSessionId = courseSessionId; }

    public boolean isPresent() { return isPresent; }
    public void setPresent(boolean isPresent) { this.isPresent = isPresent; }

    public LocalDateTime getMarkedAt() { return markedAt; }
    public void setMarkedAt(LocalDateTime markedAt) { this.markedAt = markedAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
