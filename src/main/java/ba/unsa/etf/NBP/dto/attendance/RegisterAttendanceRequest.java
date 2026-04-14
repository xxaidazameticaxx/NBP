package ba.unsa.etf.NBP.dto.attendance;

public class RegisterAttendanceRequest {

    private String sessionCode;

    public RegisterAttendanceRequest() {}

    public String getSessionCode() { return sessionCode; }
    public void setSessionCode(String sessionCode) { this.sessionCode = sessionCode; }
}
