package ba.unsa.etf.NBP.model;

import java.time.LocalDateTime;

public class CourseSession {

    private Long id;
    private Long courseId;
    private LocalDateTime sessionStartTime;
    private LocalDateTime sessionEndTime;
    private String sessionCode;
    private Long roomId;
    private Long timetableId;
    private String sessionType;

    public CourseSession() {}

    public CourseSession(Long id, Long courseId, LocalDateTime sessionStartTime,
                         LocalDateTime sessionEndTime, String sessionCode, Long roomId,
                         Long timetableId, String sessionType) {
        this.id = id;
        this.courseId = courseId;
        this.sessionStartTime = sessionStartTime;
        this.sessionEndTime = sessionEndTime;
        this.sessionCode = sessionCode;
        this.roomId = roomId;
        this.timetableId = timetableId;
        this.sessionType = sessionType;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public LocalDateTime getSessionStartTime() { return sessionStartTime; }
    public void setSessionStartTime(LocalDateTime sessionStartTime) { this.sessionStartTime = sessionStartTime; }

    public LocalDateTime getSessionEndTime() { return sessionEndTime; }
    public void setSessionEndTime(LocalDateTime sessionEndTime) { this.sessionEndTime = sessionEndTime; }

    public String getSessionCode() { return sessionCode; }
    public void setSessionCode(String sessionCode) { this.sessionCode = sessionCode; }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public Long getTimetableId() { return timetableId; }
    public void setTimetableId(Long timetableId) { this.timetableId = timetableId; }

    public String getSessionType() { return sessionType; }
    public void setSessionType(String sessionType) { this.sessionType = sessionType; }
}
