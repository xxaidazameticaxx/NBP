package ba.unsa.etf.NBP.repository;

import ba.unsa.etf.NBP.model.Student;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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

    public void save(Student student) {
        String sql = "INSERT INTO NBP_STUDENT (ID, USER_ID, INDEX_NUMBER, STUDY_PROGRAM_ID, ENROLLMENT_YEAR) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                student.getId(),
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
}
