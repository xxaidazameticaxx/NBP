package ba.unsa.etf.NBP.repository;

import ba.unsa.etf.NBP.model.Attendance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AttendanceRepository {

    private final JdbcTemplate jdbcTemplate;

    public AttendanceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Attendance> rowMapper = (rs, rowNum) -> {
        Attendance attendance = new Attendance();
        attendance.setId(rs.getLong("ID"));
        attendance.setStudentId(rs.getLong("STUDENT_ID"));
        attendance.setCourseSessionId(rs.getLong("COURSE_SESSION_ID"));
        attendance.setPresent(rs.getInt("IS_PRESENT") == 1);
        attendance.setMarkedAt(rs.getTimestamp("MARKED_AT") != null ? rs.getTimestamp("MARKED_AT").toLocalDateTime() : null);
        attendance.setNotes(rs.getString("NOTES"));
        return attendance;
    };

    public List<Attendance> findAll() {
        String sql = "SELECT * FROM NBP_ATTENDANCE";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public Optional<Attendance> findById(Long id) {
        String sql = "SELECT * FROM NBP_ATTENDANCE WHERE ID = ?";
        List<Attendance> results = jdbcTemplate.query(sql, rowMapper, id);
        return results.stream().findFirst();
    }

    public void save(Attendance attendance) {
        String sql = "INSERT INTO NBP_ATTENDANCE (ID, STUDENT_ID, COURSE_SESSION_ID, IS_PRESENT, MARKED_AT, NOTES) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                attendance.getId(),
                attendance.getStudentId(),
                attendance.getCourseSessionId(),
                attendance.isPresent() ? 1 : 0,
                attendance.getMarkedAt() != null ? java.sql.Timestamp.valueOf(attendance.getMarkedAt()) : null,
                attendance.getNotes());
    }

    public void update(Attendance attendance) {
        String sql = "UPDATE NBP_ATTENDANCE SET STUDENT_ID = ?, COURSE_SESSION_ID = ?, IS_PRESENT = ?, " +
                     "MARKED_AT = ?, NOTES = ? WHERE ID = ?";
        jdbcTemplate.update(sql,
                attendance.getStudentId(),
                attendance.getCourseSessionId(),
                attendance.isPresent() ? 1 : 0,
                attendance.getMarkedAt() != null ? java.sql.Timestamp.valueOf(attendance.getMarkedAt()) : null,
                attendance.getNotes(),
                attendance.getId());
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM NBP_ATTENDANCE WHERE ID = ?";
        jdbcTemplate.update(sql, id);
    }

    public List<Attendance> findByStudentId(Long studentId) {
        String sql = "SELECT * FROM NBP_ATTENDANCE WHERE STUDENT_ID = ?";
        return jdbcTemplate.query(sql, rowMapper, studentId);
    }

    public List<Attendance> findByCourseSessionId(Long courseSessionId) {
        String sql = "SELECT * FROM NBP_ATTENDANCE WHERE COURSE_SESSION_ID = ?";
        return jdbcTemplate.query(sql, rowMapper, courseSessionId);
    }

    public void deleteByStudentIdAndCourseId(Long studentId, Long courseId) {
        String sql = "DELETE FROM NBP_ATTENDANCE " +
                "WHERE STUDENT_ID = ? AND COURSE_SESSION_ID IN " +
                "(SELECT ID FROM NBP_COURSE_SESSION WHERE COURSE_ID = ?)";
        jdbcTemplate.update(sql, studentId, courseId);
    }
}
