package ba.unsa.etf.NBP.repository;

import ba.unsa.etf.NBP.dto.enrollment.StudentCourseDto;
import ba.unsa.etf.NBP.model.Enrollment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JDBC repository for {@link Enrollment} rows in {@code NBP_ENROLLMENT}.
 * <p>
 * Tracks student enrollment in courses with date tracking and provides
 * detailed course information with enrollment context for student views.
 */
@Repository
public class EnrollmentRepository {

    private final JdbcTemplate jdbcTemplate;

    public EnrollmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Enrollment> rowMapper = (rs, rowNum) -> {
        Enrollment enrollment = new Enrollment();
        enrollment.setId(rs.getLong("ID"));
        enrollment.setStudentId(rs.getLong("STUDENT_ID"));
        enrollment.setCourseId(rs.getLong("COURSE_ID"));
        enrollment.setEnrollmentDate(rs.getDate("ENROLLMENT_DATE") != null ? rs.getDate("ENROLLMENT_DATE").toLocalDate() : null);
        return enrollment;
    };

    public List<Enrollment> findAll() {
        String sql = "SELECT * FROM NBP_ENROLLMENT";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public Optional<Enrollment> findById(Long id) {
        String sql = "SELECT * FROM NBP_ENROLLMENT WHERE ID = ?";
        List<Enrollment> results = jdbcTemplate.query(sql, rowMapper, id);
        return results.stream().findFirst();
    }

    public void save(Enrollment enrollment) {
        String sql = "INSERT INTO NBP_ENROLLMENT (STUDENT_ID, COURSE_ID, ENROLLMENT_DATE) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql,
                enrollment.getStudentId(),
                enrollment.getCourseId(),
                enrollment.getEnrollmentDate() != null ? java.sql.Date.valueOf(enrollment.getEnrollmentDate()) : null);
    }

    public void update(Enrollment enrollment) {
        String sql = "UPDATE NBP_ENROLLMENT SET STUDENT_ID = ?, COURSE_ID = ?, ENROLLMENT_DATE = ? WHERE ID = ?";
        jdbcTemplate.update(sql,
                enrollment.getStudentId(),
                enrollment.getCourseId(),
                enrollment.getEnrollmentDate() != null ? java.sql.Date.valueOf(enrollment.getEnrollmentDate()) : null,
                enrollment.getId());
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM NBP_ENROLLMENT WHERE ID = ?";
        jdbcTemplate.update(sql, id);
    }

    public List<Enrollment> findByStudentId(Long studentId) {
        String sql = "SELECT * FROM NBP_ENROLLMENT WHERE STUDENT_ID = ?";
        return jdbcTemplate.query(sql, rowMapper, studentId);
    }

    public List<Enrollment> findByCourseId(Long courseId) {
        String sql = "SELECT * FROM NBP_ENROLLMENT WHERE COURSE_ID = ?";
        return jdbcTemplate.query(sql, rowMapper, courseId);
    }

    public boolean existsByStudentIdAndCourseId(Long studentId, Long courseId) {
        String sql = "SELECT COUNT(*) FROM NBP_ENROLLMENT WHERE STUDENT_ID = ? AND COURSE_ID = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, studentId, courseId);

        return count != null && count > 0;
    }

    public List<StudentCourseDto> findCoursesByStudentIdWithDetails(Long studentId) {
        String sql = "SELECT c.ID AS COURSE_ID, c.NAME, c.CODE, c.PROFESSOR_ID, e.ENROLLMENT_DATE " +
                "FROM NBP_ENROLLMENT e " +
                "JOIN NBP_COURSE c ON e.COURSE_ID = c.ID " +
                "WHERE e.STUDENT_ID = ?";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            StudentCourseDto dto = new StudentCourseDto();
            dto.setCourseId(rs.getLong("COURSE_ID"));
            dto.setCourseName(rs.getString("NAME"));
            dto.setCourseCode(rs.getString("CODE"));
            dto.setProfessorId(rs.getLong("PROFESSOR_ID"));

            java.sql.Date eDate = rs.getDate("ENROLLMENT_DATE");
            if (eDate != null) {
                dto.setEnrollmentDate(eDate.toLocalDate());
            }
            return dto;
        }, studentId);
    }
}
