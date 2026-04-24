package ba.unsa.etf.NBP.repository;

import ba.unsa.etf.NBP.model.Attendance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.Timestamp;

import java.util.List;
import java.util.Optional;

/**
 * JDBC repository for {@link Attendance} rows in {@code NBP_ATTENDANCE}.
 * <p>
 * Tracks student presence/absence for each course session, supports override by professors,
 * and performs batch auto-marking of absent students when a session closes.
 */
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

    public Long saveAndReturnId(Attendance attendance) {
        String sql = "INSERT INTO NBP_ATTENDANCE (STUDENT_ID, COURSE_SESSION_ID, IS_PRESENT, MARKED_AT, NOTES) " +
                "VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"ID"});
            ps.setLong(1, attendance.getStudentId());
            ps.setLong(2, attendance.getCourseSessionId());
            ps.setInt(3, attendance.isPresent() ? 1 : 0);
            ps.setTimestamp(4, attendance.getMarkedAt() != null ? Timestamp.valueOf(attendance.getMarkedAt()) : null);
            ps.setString(5, attendance.getNotes());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
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

    public Optional<Attendance> findByStudentIdAndCourseSessionId(Long studentId, Long courseSessionId) {
        String sql = "SELECT * FROM NBP_ATTENDANCE WHERE STUDENT_ID = ? AND COURSE_SESSION_ID = ?";
        List<Attendance> results = jdbcTemplate.query(sql, rowMapper, studentId, courseSessionId);
        return results.stream().findFirst();
    }

    public boolean existsByStudentIdAndCourseSessionId(Long studentId, Long courseSessionId) {
        String sql = "SELECT COUNT(*) FROM NBP_ATTENDANCE WHERE STUDENT_ID = ? AND COURSE_SESSION_ID = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, studentId, courseSessionId);
        return count != null && count > 0;
    }

    public int autoInsertAbsentForMissingEnrolledStudents(Long courseId, Long courseSessionId) {
        String sql = "INSERT INTO NBP_ATTENDANCE (STUDENT_ID, COURSE_SESSION_ID, IS_PRESENT, MARKED_AT, NOTES) " +
                "SELECT e.STUDENT_ID, ?, 0, NULL, NULL " +
                "FROM NBP_ENROLLMENT e " +
                "WHERE e.COURSE_ID = ? " +
                "AND NOT EXISTS (" +
                "  SELECT 1 FROM NBP_ATTENDANCE a " +
                "  WHERE a.STUDENT_ID = e.STUDENT_ID AND a.COURSE_SESSION_ID = ?" +
                ")";
        return jdbcTemplate.update(sql, courseSessionId, courseId, courseSessionId);
    }

    public void updateIsPresent(Long attendanceId, boolean isPresent) {
        String sql = "UPDATE NBP_ATTENDANCE SET IS_PRESENT = ? WHERE ID = ?";
        jdbcTemplate.update(sql, isPresent ? 1 : 0, attendanceId);
    }

    public List<ba.unsa.etf.NBP.dto.attendance.SessionAttendanceRecordResponse> findSessionAttendanceWithStudentNames(Long courseSessionId) {
        String sql = "SELECT a.ID AS ATTENDANCE_ID, e.STUDENT_ID, cs.ID AS COURSE_SESSION_ID, a.IS_PRESENT, a.MARKED_AT, a.NOTES, " +
            "u.FIRST_NAME, u.LAST_NAME, s.INDEX_NUMBER " +
            "FROM NBP_COURSE_SESSION cs " +
            "JOIN NBP_ENROLLMENT e ON e.COURSE_ID = cs.COURSE_ID " +
            "JOIN NBP_STUDENT s ON s.ID = e.STUDENT_ID " +
            "JOIN NBP.NBP_USER u ON u.ID = s.USER_ID " +
            "LEFT JOIN NBP_ATTENDANCE a ON a.STUDENT_ID = e.STUDENT_ID AND a.COURSE_SESSION_ID = cs.ID " +
            "WHERE cs.ID = ? " +
            "ORDER BY u.LAST_NAME, u.FIRST_NAME";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            ba.unsa.etf.NBP.dto.attendance.SessionAttendanceRecordResponse dto = new ba.unsa.etf.NBP.dto.attendance.SessionAttendanceRecordResponse();
            dto.setAttendanceId(rs.getObject("ATTENDANCE_ID", Long.class));
            dto.setStudentId(rs.getLong("STUDENT_ID"));
            dto.setFirstName(rs.getString("FIRST_NAME"));
            dto.setLastName(rs.getString("LAST_NAME"));
            dto.setIndexNumber(rs.getString("INDEX_NUMBER"));
            Integer isPresent = rs.getObject("IS_PRESENT", Integer.class);
            dto.setPresent(isPresent != null && isPresent == 1);
            dto.setMarkedAt(rs.getTimestamp("MARKED_AT") != null ? rs.getTimestamp("MARKED_AT").toLocalDateTime() : null);
            dto.setNotes(rs.getString("NOTES"));
            return dto;
        }, courseSessionId);
    }

    public void deleteByStudentIdAndCourseId(Long studentId, Long courseId) {
        String sql = "DELETE FROM NBP_ATTENDANCE " +
                "WHERE STUDENT_ID = ? AND COURSE_SESSION_ID IN " +
                "(SELECT ID FROM NBP_COURSE_SESSION WHERE COURSE_ID = ?)";
        jdbcTemplate.update(sql, studentId, courseId);
    }
}
