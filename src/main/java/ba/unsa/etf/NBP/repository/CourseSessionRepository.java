package ba.unsa.etf.NBP.repository;

import ba.unsa.etf.NBP.model.CourseSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CourseSessionRepository {

    private final JdbcTemplate jdbcTemplate;

    public CourseSessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<CourseSession> rowMapper = (rs, rowNum) -> {
        CourseSession courseSession = new CourseSession();
        courseSession.setId(rs.getLong("ID"));
        courseSession.setCourseId(rs.getLong("COURSE_ID"));
        courseSession.setSessionStartTime(rs.getTimestamp("SESSION_START_TIME") != null ? rs.getTimestamp("SESSION_START_TIME").toLocalDateTime() : null);
        courseSession.setSessionEndTime(rs.getTimestamp("SESSION_END_TIME") != null ? rs.getTimestamp("SESSION_END_TIME").toLocalDateTime() : null);
        courseSession.setSessionCode(rs.getString("SESSION_CODE"));
        courseSession.setRoomId(rs.getLong("ROOM_ID"));
        courseSession.setTimetableId(rs.getObject("TIMETABLE_ID", Long.class));
        courseSession.setSessionType(rs.getString("SESSION_TYPE"));
        return courseSession;
    };

    public List<CourseSession> findAll() {
        String sql = "SELECT * FROM NBP_COURSE_SESSION";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public Optional<CourseSession> findById(Long id) {
        String sql = "SELECT * FROM NBP_COURSE_SESSION WHERE ID = ?";
        List<CourseSession> results = jdbcTemplate.query(sql, rowMapper, id);
        return results.stream().findFirst();
    }

    public void save(CourseSession courseSession) {
        String sql = "INSERT INTO NBP_COURSE_SESSION (ID, COURSE_ID, SESSION_START_TIME, SESSION_END_TIME, SESSION_CODE, ROOM_ID, TIMETABLE_ID, SESSION_TYPE) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                courseSession.getId(),
                courseSession.getCourseId(),
                courseSession.getSessionStartTime() != null ? java.sql.Timestamp.valueOf(courseSession.getSessionStartTime()) : null,
                courseSession.getSessionEndTime() != null ? java.sql.Timestamp.valueOf(courseSession.getSessionEndTime()) : null,
                courseSession.getSessionCode(),
                courseSession.getRoomId(),
                courseSession.getTimetableId(),
                courseSession.getSessionType());
    }

    public void update(CourseSession courseSession) {
        String sql = "UPDATE NBP_COURSE_SESSION SET COURSE_ID = ?, SESSION_START_TIME = ?, SESSION_END_TIME = ?, " +
                     "SESSION_CODE = ?, ROOM_ID = ?, TIMETABLE_ID = ?, SESSION_TYPE = ? WHERE ID = ?";
        jdbcTemplate.update(sql,
                courseSession.getCourseId(),
                courseSession.getSessionStartTime() != null ? java.sql.Timestamp.valueOf(courseSession.getSessionStartTime()) : null,
                courseSession.getSessionEndTime() != null ? java.sql.Timestamp.valueOf(courseSession.getSessionEndTime()) : null,
                courseSession.getSessionCode(),
                courseSession.getRoomId(),
                courseSession.getTimetableId(),
                courseSession.getSessionType(),
                courseSession.getId());
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM NBP_COURSE_SESSION WHERE ID = ?";
        jdbcTemplate.update(sql, id);
    }
}
