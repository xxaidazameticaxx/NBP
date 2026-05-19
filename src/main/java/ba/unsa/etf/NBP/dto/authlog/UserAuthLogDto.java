package ba.unsa.etf.NBP.dto.authlog;

import ba.unsa.etf.NBP.model.AuthLogAction;

import java.time.Instant;

/**
 * DTO representation of a single authentication event returned by the API.
 */
public class UserAuthLogDto {

    private Instant timestamp;
    private AuthLogAction action;

    public UserAuthLogDto() {
    }

    public UserAuthLogDto(Instant timestamp, AuthLogAction action) {
        this.timestamp = timestamp;
        this.action = action;
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
