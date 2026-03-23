package ba.unsa.etf.NBP.repository;

import ba.unsa.etf.NBP.model.Course;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CourseRepository {

    private final JdbcTemplate jdbcTemplate;

    public CourseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Course> rowMapper = (rs, rowNum) -> {
        Course course = new Course();
        course.setId(rs.getLong("ID"));
        course.setName(rs.getString("NAME"));
        course.setCode(rs.getString("CODE"));
        course.setProfessorId(rs.getLong("PROFESSOR_ID"));
        course.setDepartmentId(rs.getLong("DEPARTMENT_ID"));
        course.setAcademicYear(rs.getString("ACADEMIC_YEAR"));
        course.setSemester(rs.getObject("SEMESTER", Long.class));
        course.setCredits(rs.getObject("CREDITS", Long.class));
        return course;
    };

    public List<Course> findAll() {
        String sql = "SELECT * FROM NBP_COURSE";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public Optional<Course> findById(Long id) {
        String sql = "SELECT * FROM NBP_COURSE WHERE ID = ?";
        List<Course> results = jdbcTemplate.query(sql, rowMapper, id);
        return results.stream().findFirst();
    }

    public void save(Course course) {
        String sql = "INSERT INTO NBP_COURSE (ID, NAME, CODE, PROFESSOR_ID, DEPARTMENT_ID, ACADEMIC_YEAR, SEMESTER, CREDITS) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                course.getId(),
                course.getName(),
                course.getCode(),
                course.getProfessorId(),
                course.getDepartmentId(),
                course.getAcademicYear(),
                course.getSemester(),
                course.getCredits());
    }

    public void update(Course course) {
        String sql = "UPDATE NBP_COURSE SET NAME = ?, CODE = ?, PROFESSOR_ID = ?, DEPARTMENT_ID = ?, " +
                     "ACADEMIC_YEAR = ?, SEMESTER = ?, CREDITS = ? WHERE ID = ?";
        jdbcTemplate.update(sql,
                course.getName(),
                course.getCode(),
                course.getProfessorId(),
                course.getDepartmentId(),
                course.getAcademicYear(),
                course.getSemester(),
                course.getCredits(),
                course.getId());
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM NBP_COURSE WHERE ID = ?";
        jdbcTemplate.update(sql, id);
    }

    public List<Course> findByProfessorId(Long professorId) {
        String sql = "SELECT * FROM NBP_COURSE WHERE PROFESSOR_ID = ?";
        return jdbcTemplate.query(sql, rowMapper, professorId);
    }

    public List<Course> findByDepartmentId(Long departmentId) {
        String sql = "SELECT * FROM NBP_COURSE WHERE DEPARTMENT_ID = ?";
        return jdbcTemplate.query(sql, rowMapper, departmentId);
    }
}
