package ba.unsa.etf.NBP.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Authentication event stored in MongoDB.
 */
@Document(collection = "user_auth_logs")
public class UserAuthLog {

    @Id
    private String id;

    private Long userId;

    private Instant timestamp;

    private AuthLogAction action;

    public UserAuthLog() {
    }

    public UserAuthLog(Long userId, Instant timestamp, AuthLogAction action) {
        this.userId = userId;
        this.timestamp = timestamp;
        this.action = action;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public AuthLogAction getAction() {
        return action;
    }

    public void setAction(AuthLogAction action) {
        this.action = action;
    }
}
