package ba.unsa.etf.NBP.repository;

import ba.unsa.etf.NBP.dto.enrollment.EnrolledStudentDto;
import ba.unsa.etf.NBP.model.Student;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JDBC repository for {@link Student} rows in {@code NBP_STUDENT}.
 * <p>
 * Links student records to users and study programs, providing roster lookups
 * for course management and authorization checks on attendance access.
 */
@Repository
public class StudentRepository {

    private final JdbcTemplate jdbcTemplate;

    public StudentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Student> rowMapper = (rs, rowNum) -> {
        Student student = new Student();
        student.setId(rs.getLong("ID"));
        student.setUserId(rs.getLong("USER_ID"));
        student.setIndexNumber(rs.getString("INDEX_NUMBER"));
        student.setStudyProgramId(rs.getLong("STUDY_PROGRAM_ID"));
        student.setEnrollmentYear(rs.getObject("ENROLLMENT_YEAR", Long.class));
        return student;
    };

    public List<Student> findAll() {
        String sql = "SELECT * FROM NBP_STUDENT";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public Optional<Student> findById(Long id) {
        String sql = "SELECT * FROM NBP_STUDENT WHERE ID = ?";
        List<Student> results = jdbcTemplate.query(sql, rowMapper, id);
        return results.stream().findFirst();
    }

    public Optional<Student> findByUserId(Long userId) {
        String sql = "SELECT * FROM NBP_STUDENT WHERE USER_ID = ?";
        List<Student> results = jdbcTemplate.query(sql, rowMapper, userId);
        return results.stream().findFirst();
    }

    public void save(Student student) {
        String sql = "INSERT INTO NBP_STUDENT (USER_ID, INDEX_NUMBER, STUDY_PROGRAM_ID, ENROLLMENT_YEAR) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                student.getUserId(),
                student.getIndexNumber(),
                student.getStudyProgramId(),
                student.getEnrollmentYear());
    }

    public void update(Student student) {
        String sql = "UPDATE NBP_STUDENT SET USER_ID = ?, INDEX_NUMBER = ?, STUDY_PROGRAM_ID = ?, ENROLLMENT_YEAR = ? WHERE ID = ?";
        jdbcTemplate.update(sql,
                student.getUserId(),
                student.getIndexNumber(),
                student.getStudyProgramId(),
                student.getEnrollmentYear(),
                student.getId());
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM NBP_STUDENT WHERE ID = ?";
        jdbcTemplate.update(sql, id);
    }

    public List<EnrolledStudentDto> findRosterByCourseId(Long courseId) {
        String sql = "SELECT s.ID AS STUDENT_ID, s.USER_ID, s.INDEX_NUMBER, s.ENROLLMENT_YEAR, " +
                "e.ENROLLMENT_DATE, " +
                "sp.ID AS STUDY_PROGRAM_ID, sp.NAME AS STUDY_PROGRAM_NAME, sp.CODE AS STUDY_PROGRAM_CODE " +
                "FROM NBP_ENROLLMENT e " +
                "JOIN NBP_STUDENT s ON e.STUDENT_ID = s.ID " +
                "JOIN NBP_STUDY_PROGRAM sp ON s.STUDY_PROGRAM_ID = sp.ID " +
                "WHERE e.COURSE_ID = ?";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            EnrolledStudentDto dto = new EnrolledStudentDto();
            dto.setStudentId(rs.getLong("STUDENT_ID"));
            dto.setUserId(rs.getLong("USER_ID"));
            dto.setIndexNumber(rs.getString("INDEX_NUMBER"));
            dto.setEnrollmentYear(rs.getObject("ENROLLMENT_YEAR", Long.class));

            java.sql.Date enrollmentDate = rs.getDate("ENROLLMENT_DATE");
            if (enrollmentDate != null) {
                dto.setEnrollmentDate(enrollmentDate.toLocalDate());
            }

            dto.setStudyProgramId(rs.getLong("STUDY_PROGRAM_ID"));
            dto.setStudyProgramName(rs.getString("STUDY_PROGRAM_NAME"));
            dto.setStudyProgramCode(rs.getString("STUDY_PROGRAM_CODE"));
            return dto;
        }, courseId);
    }
}
