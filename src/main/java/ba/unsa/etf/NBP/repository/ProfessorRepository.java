package ba.unsa.etf.NBP.repository;

import ba.unsa.etf.NBP.model.Professor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ProfessorRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProfessorRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Professor> rowMapper = (rs, rowNum) -> {
        Professor professor = new Professor();
        professor.setId(rs.getLong("ID"));
        professor.setUserId(rs.getLong("USER_ID"));
        professor.setTitle(rs.getString("TITLE"));
        professor.setDepartmentId(rs.getLong("DEPARTMENT_ID"));
        professor.setOfficeLocation(rs.getString("OFFICE_LOCATION"));
        return professor;
    };

    public List<Professor> findAll() {
        String sql = "SELECT * FROM NBP_PROFESSOR";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public Optional<Professor> findById(Long id) {
        String sql = "SELECT * FROM NBP_PROFESSOR WHERE ID = ?";
        List<Professor> results = jdbcTemplate.query(sql, rowMapper, id);
        return results.stream().findFirst();
    }

    public Optional<Professor> findByUserId(Long userId) {
        String sql = "SELECT * FROM NBP_PROFESSOR WHERE USER_ID = ?";
        List<Professor> results = jdbcTemplate.query(sql, rowMapper, userId);
        return results.stream().findFirst();
    }

    public void save(Professor professor) {
        String sql = "INSERT INTO NBP_PROFESSOR (ID, USER_ID, TITLE, DEPARTMENT_ID, OFFICE_LOCATION) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                professor.getId(),
                professor.getUserId(),
                professor.getTitle(),
                professor.getDepartmentId(),
                professor.getOfficeLocation());
    }

    public void update(Professor professor) {
        String sql = "UPDATE NBP_PROFESSOR SET USER_ID = ?, TITLE = ?, DEPARTMENT_ID = ?, OFFICE_LOCATION = ? WHERE ID = ?";
        jdbcTemplate.update(sql,
                professor.getUserId(),
                professor.getTitle(),
                professor.getDepartmentId(),
                professor.getOfficeLocation(),
                professor.getId());
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM NBP_PROFESSOR WHERE ID = ?";
        jdbcTemplate.update(sql, id);
    }
}
