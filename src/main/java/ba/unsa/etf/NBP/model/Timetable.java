package ba.unsa.etf.NBP.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Timetable {

    private Long id;
    private Long courseId;
    private Long roomId;
    private String dayOfWeek;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDate validFrom;
    private LocalDate validTo;

    public Timetable() {}

    public Timetable(Long id, Long courseId, Long roomId, String dayOfWeek,
                     LocalDateTime startTime, LocalDateTime endTime,
                     LocalDate validFrom, LocalDate validTo) {
        this.id = id;
        this.courseId = courseId;
        this.roomId = roomId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }

    public LocalDate getValidTo() { return validTo; }
    public void setValidTo(LocalDate validTo) { this.validTo = validTo; }
}
