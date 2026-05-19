package ba.unsa.etf.NBP.dto.authlog;

import java.util.List;

/**
 * API response that contains user details and their authentication logs.
 */
public class UserAuthLogsResponse {

    private Long userId;
    private String username;
    private String firstName;
    private String lastName;
    private String email;

    private List<UserAuthLogDto> logs;

    public UserAuthLogsResponse() {
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<UserAuthLogDto> getLogs() {
        return logs;
    }

    public void setLogs(List<UserAuthLogDto> logs) {
        this.logs = logs;
    }
}
