package ba.unsa.etf.NBP.dto.session;

public class OpenSessionRequest {

    private Long timetableId;
    private Long roomId;

    public OpenSessionRequest() {}

    public Long getTimetableId() { return timetableId; }
    public void setTimetableId(Long timetableId) { this.timetableId = timetableId; }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
}
