package ba.unsa.etf.NBP.repository;

import ba.unsa.etf.NBP.model.AbsenceExcuse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Blob;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * JDBC repository for {@link AbsenceExcuse} rows in {@code NBP_ABSENCE_EXCUSE}.
 * <p>
 * Handles persistence of student excuse submissions with PDF document storage,
 * status tracking (PENDING, APPROVED, REJECTED), and professor review lookups.
 */
@Repository
public class AbsenceExcuseRepository {
    private final JdbcTemplate jdbcTemplate;
    public AbsenceExcuseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    /**
     * Maps database rows to {@link AbsenceExcuse} objects.
     */
    private final RowMapper<AbsenceExcuse> rowMapper = (rs, rowNum) -> {
        AbsenceExcuse absenceExcuse = new AbsenceExcuse();
        absenceExcuse.setId(rs.getLong("ID"));
        absenceExcuse.setStudentId(rs.getLong("STUDENT_ID"));
        absenceExcuse.setCourseSessionId(rs.getLong("COURSE_SESSION_ID"));
        absenceExcuse.setReason(rs.getString("REASON"));
        absenceExcuse.setSubmittedAt(rs.getTimestamp("SUBMITTED_AT") != null
                ? rs.getTimestamp("SUBMITTED_AT").toLocalDateTime() : null);
        absenceExcuse.setStatus(rs.getString("STATUS"));
        absenceExcuse.setReviewedBy(rs.getObject("REVIEWED_BY", Long.class));
        Blob blob = rs.getBlob("DOCUMENT");
        if (blob != null) {
            absenceExcuse.setDocument(blob.getBytes(1, (int) blob.length()));
        }
        absenceExcuse.setDocumentName(rs.getString("DOCUMENT_NAME"));
        return absenceExcuse;
    };
    /**
     * Retrieves all absence excuses.
     */
    public List<AbsenceExcuse> findAll() {
        String sql = "SELECT * FROM NBP_ABSENCE_EXCUSE";
        return jdbcTemplate.query(sql, rowMapper);
    }

    /**
     * Retrieves an excuse by ID.
     */
    public Optional<AbsenceExcuse> findById(Long id) {
        String sql = "SELECT * FROM NBP_ABSENCE_EXCUSE WHERE ID = ?";
        List<AbsenceExcuse> results = jdbcTemplate.query(sql, rowMapper, id);
        return results.stream().findFirst();
    }

    /**
     * Inserts an excuse row (low level).
     */
    public void save(AbsenceExcuse absenceExcuse) {
        String sql = "INSERT INTO NBP_ABSENCE_EXCUSE (ID, STUDENT_ID, COURSE_SESSION_ID, REASON, SUBMITTED_AT, STATUS, REVIEWED_BY, DOCUMENT, DOCUMENT_NAME) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                absenceExcuse.getId(),
                absenceExcuse.getStudentId(),
                absenceExcuse.getCourseSessionId(),
                absenceExcuse.getReason(),
                absenceExcuse.getSubmittedAt() != null ? java.sql.Timestamp.valueOf(absenceExcuse.getSubmittedAt()) : null,
                absenceExcuse.getStatus(),
                absenceExcuse.getReviewedBy(),
                absenceExcuse.getDocument(),
                absenceExcuse.getDocumentName());
    }
    /**
     * Updates an excuse row.
     */
    public void update(AbsenceExcuse absenceExcuse) {
        String sql = "UPDATE NBP_ABSENCE_EXCUSE SET STUDENT_ID = ?, COURSE_SESSION_ID = ?, REASON = ?, " +
                     "SUBMITTED_AT = ?, STATUS = ?, REVIEWED_BY = ?, DOCUMENT = ?, DOCUMENT_NAME = ? WHERE ID = ?";
        jdbcTemplate.update(sql,
                absenceExcuse.getStudentId(),
                absenceExcuse.getCourseSessionId(),
                absenceExcuse.getReason(),
                absenceExcuse.getSubmittedAt() != null ? java.sql.Timestamp.valueOf(absenceExcuse.getSubmittedAt()) : null,
                absenceExcuse.getStatus(),
                absenceExcuse.getReviewedBy(),
                absenceExcuse.getDocument(),
                absenceExcuse.getDocumentName(),
                absenceExcuse.getId());
    }
    /**
     * Deletes an excuse by ID.
     */
    public void deleteById(Long id) {
        String sql = "DELETE FROM NBP_ABSENCE_EXCUSE WHERE ID = ?";
        jdbcTemplate.update(sql, id);
    }

    /**
     * Inserts an excuse and returns its assigned ID.
     */
    public Long saveAndReturnId(AbsenceExcuse absenceExcuse) {
        String sql = "INSERT INTO NBP_ABSENCE_EXCUSE " +
                     "(STUDENT_ID, COURSE_SESSION_ID, REASON, SUBMITTED_AT, STATUS, REVIEWED_BY, DOCUMENT, DOCUMENT_NAME) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"ID"});
            ps.setLong(1, absenceExcuse.getStudentId());
            ps.setLong(2, absenceExcuse.getCourseSessionId());
            ps.setString(3, absenceExcuse.getReason());
            ps.setTimestamp(4, absenceExcuse.getSubmittedAt() != null
                    ? java.sql.Timestamp.valueOf(absenceExcuse.getSubmittedAt()) : null);
            ps.setString(5, absenceExcuse.getStatus());
            ps.setObject(6, absenceExcuse.getReviewedBy());
            ps.setBytes(7, absenceExcuse.getDocument());
            ps.setString(8, absenceExcuse.getDocumentName());
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }
    /**
     * Retrieves excuses by student ID.
     */
    public List<AbsenceExcuse> findByStudentId(Long studentId) {
        String sql = "SELECT * FROM NBP_ABSENCE_EXCUSE WHERE STUDENT_ID = ?";
        return jdbcTemplate.query(sql, rowMapper, studentId);
    }

    /**
     * Retrieves excuses by course session ID.
     */
    public List<AbsenceExcuse> findByCourseSessionId(Long courseSessionId) {
        String sql = "SELECT * FROM NBP_ABSENCE_EXCUSE WHERE COURSE_SESSION_ID = ?";
        return jdbcTemplate.query(sql, rowMapper, courseSessionId);
    }

    /**
     * Retrieves an excuse for a specific student and session.
     */
    public Optional<AbsenceExcuse> findByStudentIdAndCourseSessionId(Long studentId, Long courseSessionId) {
        String sql = "SELECT * FROM NBP_ABSENCE_EXCUSE WHERE STUDENT_ID = ? AND COURSE_SESSION_ID = ?";
        List<AbsenceExcuse> results = jdbcTemplate.query(sql, rowMapper, studentId, courseSessionId);
        return results.stream().findFirst();
    }

    /**
     * Retrieves pending excuses for a professor's courses.
     */
    public List<AbsenceExcuse> findPendingByProfessorId(Long professorId) {
        String sql = "SELECT ae.* FROM NBP_ABSENCE_EXCUSE ae " +
                     "JOIN NBP_COURSE_SESSION cs ON ae.COURSE_SESSION_ID = cs.ID " +
                     "JOIN NBP_COURSE c ON cs.COURSE_ID = c.ID " +
                     "WHERE ae.STATUS = 'PENDING' AND c.PROFESSOR_ID = ?";
        return jdbcTemplate.query(sql, rowMapper, professorId);
    }
}
