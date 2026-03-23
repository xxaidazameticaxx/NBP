package ba.unsa.etf.NBP.repository;

import ba.unsa.etf.NBP.model.AbsenceExcuse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AbsenceExcuseRepository {

    private final JdbcTemplate jdbcTemplate;

    public AbsenceExcuseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<AbsenceExcuse> rowMapper = (rs, rowNum) -> {
        AbsenceExcuse absenceExcuse = new AbsenceExcuse();
        absenceExcuse.setId(rs.getLong("ID"));
        absenceExcuse.setStudentId(rs.getLong("STUDENT_ID"));
        absenceExcuse.setCourseSessionId(rs.getLong("COURSE_SESSION_ID"));
        absenceExcuse.setReason(rs.getString("REASON"));
        absenceExcuse.setSubmittedAt(rs.getTimestamp("SUBMITTED_AT") != null ? rs.getTimestamp("SUBMITTED_AT").toLocalDateTime() : null);
        absenceExcuse.setStatus(rs.getString("STATUS"));
        absenceExcuse.setReviewedBy(rs.getObject("REVIEWED_BY", Long.class));
        return absenceExcuse;
    };

    public List<AbsenceExcuse> findAll() {
        String sql = "SELECT * FROM NBP_ABSENCE_EXCUSE";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public Optional<AbsenceExcuse> findById(Long id) {
        String sql = "SELECT * FROM NBP_ABSENCE_EXCUSE WHERE ID = ?";
        List<AbsenceExcuse> results = jdbcTemplate.query(sql, rowMapper, id);
        return results.stream().findFirst();
    }

    public void save(AbsenceExcuse absenceExcuse) {
        String sql = "INSERT INTO NBP_ABSENCE_EXCUSE (ID, STUDENT_ID, COURSE_SESSION_ID, REASON, SUBMITTED_AT, STATUS, REVIEWED_BY) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                absenceExcuse.getId(),
                absenceExcuse.getStudentId(),
                absenceExcuse.getCourseSessionId(),
                absenceExcuse.getReason(),
                absenceExcuse.getSubmittedAt() != null ? java.sql.Timestamp.valueOf(absenceExcuse.getSubmittedAt()) : null,
                absenceExcuse.getStatus(),
                absenceExcuse.getReviewedBy());
    }

    public void update(AbsenceExcuse absenceExcuse) {
        String sql = "UPDATE NBP_ABSENCE_EXCUSE SET STUDENT_ID = ?, COURSE_SESSION_ID = ?, REASON = ?, " +
                     "SUBMITTED_AT = ?, STATUS = ?, REVIEWED_BY = ? WHERE ID = ?";
        jdbcTemplate.update(sql,
                absenceExcuse.getStudentId(),
                absenceExcuse.getCourseSessionId(),
                absenceExcuse.getReason(),
                absenceExcuse.getSubmittedAt() != null ? java.sql.Timestamp.valueOf(absenceExcuse.getSubmittedAt()) : null,
                absenceExcuse.getStatus(),
                absenceExcuse.getReviewedBy(),
                absenceExcuse.getId());
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM NBP_ABSENCE_EXCUSE WHERE ID = ?";
        jdbcTemplate.update(sql, id);
    }
}
