package ba.unsa.etf.NBP.repository;

import ba.unsa.etf.NBP.model.Department;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JDBC repository for {@link Department} rows in {@code NBP_DEPARTMENT}.
 * <p>
 * Manages academic departments, providing CRUD operations for department metadata.
 */
@Repository
public class DepartmentRepository {

    private final JdbcTemplate jdbcTemplate;

    public DepartmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Department> rowMapper = (rs, rowNum) -> {
        Department department = new Department();
        department.setId(rs.getLong("ID"));
        department.setName(rs.getString("NAME"));
        department.setCode(rs.getString("CODE"));
        department.setDescription(rs.getString("DESCRIPTION"));
        return department;
    };

    public List<Department> findAll() {
        String sql = "SELECT * FROM NBP_DEPARTMENT";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public Optional<Department> findById(Long id) {
        String sql = "SELECT * FROM NBP_DEPARTMENT WHERE ID = ?";
        List<Department> results = jdbcTemplate.query(sql, rowMapper, id);
        return results.stream().findFirst();
    }

    public void save(Department department) {
        String sql = "INSERT INTO NBP_DEPARTMENT (ID, NAME, CODE, DESCRIPTION) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                department.getId(),
                department.getName(),
                department.getCode(),
                department.getDescription());
    }

    public void update(Department department) {
        String sql = "UPDATE NBP_DEPARTMENT SET NAME = ?, CODE = ?, DESCRIPTION = ? WHERE ID = ?";
        jdbcTemplate.update(sql,
                department.getName(),
                department.getCode(),
                department.getDescription(),
                department.getId());
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM NBP_DEPARTMENT WHERE ID = ?";
        jdbcTemplate.update(sql, id);
    }
}
