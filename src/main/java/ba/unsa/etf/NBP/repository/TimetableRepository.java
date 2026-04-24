package ba.unsa.etf.NBP.repository;

import ba.unsa.etf.NBP.model.Timetable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JDBC repository for {@link Timetable} rows in {@code NBP_TIMETABLE}.
 * <p>
 * Manages scheduled class slots with time, room, and validity period tracking,
 * supporting course session room assignment and schedule management.
 */
@Repository
public class TimetableRepository {

    private final JdbcTemplate jdbcTemplate;

    public TimetableRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Timetable> rowMapper = (rs, rowNum) -> {
        Timetable timetable = new Timetable();
        timetable.setId(rs.getLong("ID"));
        timetable.setCourseId(rs.getLong("COURSE_ID"));
        timetable.setRoomId(rs.getLong("ROOM_ID"));
        timetable.setDayOfWeek(rs.getString("DAY_OF_WEEK"));
        timetable.setStartTime(rs.getTimestamp("START_TIME") != null ? rs.getTimestamp("START_TIME").toLocalDateTime() : null);
        timetable.setEndTime(rs.getTimestamp("END_TIME") != null ? rs.getTimestamp("END_TIME").toLocalDateTime() : null);
        timetable.setValidFrom(rs.getDate("VALID_FROM") != null ? rs.getDate("VALID_FROM").toLocalDate() : null);
        timetable.setValidTo(rs.getDate("VALID_TO") != null ? rs.getDate("VALID_TO").toLocalDate() : null);
        return timetable;
    };

    public List<Timetable> findAll() {
        String sql = "SELECT * FROM NBP_TIMETABLE";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public Optional<Timetable> findById(Long id) {
        String sql = "SELECT * FROM NBP_TIMETABLE WHERE ID = ?";
        List<Timetable> results = jdbcTemplate.query(sql, rowMapper, id);
        return results.stream().findFirst();
    }

    public void save(Timetable timetable) {
        String sql = "INSERT INTO NBP_TIMETABLE (ID, COURSE_ID, ROOM_ID, DAY_OF_WEEK, START_TIME, END_TIME, VALID_FROM, VALID_TO) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                timetable.getId(),
                timetable.getCourseId(),
                timetable.getRoomId(),
                timetable.getDayOfWeek(),
                timetable.getStartTime() != null ? java.sql.Timestamp.valueOf(timetable.getStartTime()) : null,
                timetable.getEndTime() != null ? java.sql.Timestamp.valueOf(timetable.getEndTime()) : null,
                timetable.getValidFrom() != null ? java.sql.Date.valueOf(timetable.getValidFrom()) : null,
                timetable.getValidTo() != null ? java.sql.Date.valueOf(timetable.getValidTo()) : null);
    }

    public void update(Timetable timetable) {
        String sql = "UPDATE NBP_TIMETABLE SET COURSE_ID = ?, ROOM_ID = ?, DAY_OF_WEEK = ?, START_TIME = ?, " +
                     "END_TIME = ?, VALID_FROM = ?, VALID_TO = ? WHERE ID = ?";
        jdbcTemplate.update(sql,
                timetable.getCourseId(),
                timetable.getRoomId(),
                timetable.getDayOfWeek(),
                timetable.getStartTime() != null ? java.sql.Timestamp.valueOf(timetable.getStartTime()) : null,
                timetable.getEndTime() != null ? java.sql.Timestamp.valueOf(timetable.getEndTime()) : null,
                timetable.getValidFrom() != null ? java.sql.Date.valueOf(timetable.getValidFrom()) : null,
                timetable.getValidTo() != null ? java.sql.Date.valueOf(timetable.getValidTo()) : null,
                timetable.getId());
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM NBP_TIMETABLE WHERE ID = ?";
        jdbcTemplate.update(sql, id);
    }
}
