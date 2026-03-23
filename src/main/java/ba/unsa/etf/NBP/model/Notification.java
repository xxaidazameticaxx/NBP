package ba.unsa.etf.NBP.model;

import java.time.LocalDateTime;

public class Notification {

    private Long id;
    private Long userId;
    private String title;
    private String message;
    private boolean isRead;
    private LocalDateTime createdAt;
    private String notificationType;
    private Long courseSessionId;

    public Notification() {}

    public Notification(Long id, Long userId, String title, String message, boolean isRead,
                        LocalDateTime createdAt, String notificationType, Long courseSessionId) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.isRead = isRead;
        this.createdAt = createdAt;
        this.notificationType = notificationType;
        this.courseSessionId = courseSessionId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean isRead) { this.isRead = isRead; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getNotificationType() { return notificationType; }
    public void setNotificationType(String notificationType) { this.notificationType = notificationType; }

    public Long getCourseSessionId() { return courseSessionId; }
    public void setCourseSessionId(Long courseSessionId) { this.courseSessionId = courseSessionId; }
}
