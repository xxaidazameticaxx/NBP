package ba.unsa.etf.NBP.repository;

import ba.unsa.etf.NBP.model.AttendanceAlert;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class AttendanceAlertRepository {

    private final JdbcTemplate jdbcTemplate;

    public AttendanceAlertRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<AttendanceAlert> rowMapper = (rs, rowNum) -> {
        AttendanceAlert alert = new AttendanceAlert();
        alert.setId(rs.getLong("ID"));
        alert.setStudentId(rs.getLong("STUDENT_ID"));
        alert.setCourseId(rs.getLong("COURSE_ID"));
        alert.setRiskLevel(rs.getString("RISK_LEVEL"));
        alert.setAttendanceRate(rs.getDouble("ATTENDANCE_RATE"));
        alert.setWeeklyTrend(rs.getDouble("WEEKLY_TREND"));
        alert.setAlertReason(rs.getString("ALERT_REASON"));
        alert.setCreatedAt(rs.getTimestamp("CREATED_AT") != null ? rs.getTimestamp("CREATED_AT").toLocalDateTime() : null);
        alert.setResolved(rs.getObject("IS_RESOLVED") != null && rs.getInt("IS_RESOLVED") == 1);
        alert.setActionTaken(rs.getString("ACTION_TAKEN"));
        alert.setResolvedAt(rs.getTimestamp("RESOLVED_AT") != null ? rs.getTimestamp("RESOLVED_AT").toLocalDateTime() : null);
        alert.setWeeksAnalyzed(rs.getObject("WEEKS_ANALYZED", Integer.class));
        return alert;
    };

    public Long save(AttendanceAlert alert) {
        String sql = "INSERT INTO NBP_ATTENDANCE_ALERT " +
                "(STUDENT_ID, COURSE_ID, RISK_LEVEL, ATTENDANCE_RATE, WEEKLY_TREND, ALERT_REASON, " +
                "CREATED_AT, IS_RESOLVED, WEEKS_ANALYZED) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"ID"});
            ps.setLong(1, alert.getStudentId());
            ps.setLong(2, alert.getCourseId());
            ps.setString(3, alert.getRiskLevel());
            ps.setDouble(4, alert.getAttendanceRate());
            ps.setDouble(5, alert.getWeeklyTrend());
            ps.setString(6, alert.getAlertReason());
            ps.setTimestamp(7, Timestamp.valueOf(alert.getCreatedAt()));
            ps.setInt(8, alert.isResolved() ? 1 : 0);
            ps.setObject(9, alert.getWeeksAnalyzed());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public Optional<AttendanceAlert> findById(Long id) {
        String sql = "SELECT * FROM NBP_ATTENDANCE_ALERT WHERE ID = ?";
        List<AttendanceAlert> results = jdbcTemplate.query(sql, rowMapper, id);
        return results.stream().findFirst();
    }

    public List<AttendanceAlert> findUnresolvedByStudentAndCourse(Long studentId, Long courseId) {
        String sql = "SELECT * FROM NBP_ATTENDANCE_ALERT " +
                "WHERE STUDENT_ID = ? AND COURSE_ID = ? AND IS_RESOLVED = 0 " +
                "ORDER BY CREATED_AT DESC";
        return jdbcTemplate.query(sql, rowMapper, studentId, courseId);
    }

    public List<AttendanceAlert> findByCourseId(Long courseId) {
        String sql = "SELECT * FROM NBP_ATTENDANCE_ALERT " +
                "WHERE COURSE_ID = ? AND IS_RESOLVED = 0 " +
                "ORDER BY RISK_LEVEL DESC, CREATED_AT DESC";
        return jdbcTemplate.query(sql, rowMapper, courseId);
    }

    public List<AttendanceAlert> findByStudentId(Long studentId) {
        String sql = "SELECT * FROM NBP_ATTENDANCE_ALERT " +
                "WHERE STUDENT_ID = ? AND IS_RESOLVED = 0 " +
                "ORDER BY CREATED_AT DESC";
        return jdbcTemplate.query(sql, rowMapper, studentId);
    }

    public List<AttendanceAlert> findAll() {
        String sql = "SELECT * FROM NBP_ATTENDANCE_ALERT WHERE IS_RESOLVED = 0 ORDER BY CREATED_AT DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public void markResolved(Long alertId, String actionTaken) {
        String sql = "UPDATE NBP_ATTENDANCE_ALERT " +
                "SET IS_RESOLVED = 1, ACTION_TAKEN = ?, RESOLVED_AT = ? " +
                "WHERE ID = ?";
        jdbcTemplate.update(sql, actionTaken, Timestamp.valueOf(java.time.LocalDateTime.now()), alertId);
    }

    public boolean existsUnresolvedForStudentAndCourse(Long studentId, Long courseId) {
        String sql = "SELECT COUNT(*) FROM NBP_ATTENDANCE_ALERT " +
                "WHERE STUDENT_ID = ? AND COURSE_ID = ? AND IS_RESOLVED = 0";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, studentId, courseId);
        return count != null && count > 0;
    }

    public Optional<AttendanceAlert> findMostRecentlyResolvedAlert(Long studentId, Long courseId) {
        String sql = "SELECT * FROM NBP_ATTENDANCE_ALERT " +
                "WHERE STUDENT_ID = ? AND COURSE_ID = ? AND IS_RESOLVED = 1 " +
                "ORDER BY RESOLVED_AT DESC LIMIT 1";
        List<AttendanceAlert> results = jdbcTemplate.query(sql, rowMapper, studentId, courseId);
        return results.stream().findFirst();
    }
}
