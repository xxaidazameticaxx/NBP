package ba.unsa.etf.NBP.repository;

import ba.unsa.etf.NBP.model.Role;
import ba.unsa.etf.NBP.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

/**
 * JDBC repository for {@link User} rows in {@code NBP_USER}.
 * <p>
 * Manages user accounts with role associations, providing lookups by username
 * and email for authentication and authorization.
 */
@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<User> rowMapper = (rs, rowNum) -> {
        Role role = null;
        Long roleId = rs.getObject("ROLE_ID", Long.class);
        if (roleId != null) {
            role = new Role();
            role.setId(roleId);
            role.setRoleName(rs.getString("NAME"));
        }

        User user = new User();
        user.setId(rs.getLong("ID"));
        user.setUsername(rs.getString("USERNAME"));
        user.setPassword(rs.getString("PASSWORD"));
        user.setFirstName(rs.getString("FIRST_NAME"));
        user.setLastName(rs.getString("LAST_NAME"));
        user.setEmail(rs.getString("EMAIL"));
        Date birthDate = rs.getDate("BIRTH_DATE");
        user.setBirthDate(birthDate != null ? birthDate.toLocalDate() : null);
        user.setAddressId(rs.getObject("ADDRESS_ID", Long.class));
        user.setRole(role);
        return user;
    };

    public Optional<User> findByUsername(String username) {
        String sql = """
                SELECT U.ID, U.USERNAME, U.PASSWORD, U.FIRST_NAME, U.LAST_NAME, U.EMAIL, U.BIRTH_DATE, U.ADDRESS_ID, U.ROLE_ID, R.NAME
                FROM NBP.NBP_USER U
                LEFT JOIN NBP.NBP_ROLE R ON R.ID = U.ROLE_ID
                WHERE U.USERNAME = ?
                """;
        List<User> results = jdbcTemplate.query(sql, rowMapper, username);
        return results.stream().findFirst();
    }

    public Optional<User> findById(Long id) {
        String sql = """
                SELECT U.ID, U.USERNAME, U.PASSWORD, U.FIRST_NAME, U.LAST_NAME, U.EMAIL, U.BIRTH_DATE, U.ADDRESS_ID, U.ROLE_ID, R.NAME
                FROM NBP.NBP_USER U
                LEFT JOIN NBP.NBP_ROLE R ON R.ID = U.ROLE_ID
                WHERE U.ID = ?
                """;
        List<User> results = jdbcTemplate.query(sql, rowMapper, id);
        return results.stream().findFirst();
    }

    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(1) FROM NBP.NBP_USER WHERE USERNAME = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, username);
        return count != null && count > 0;
    }

    public Optional<Role> findRoleById(Long roleId) {
        String sql = "SELECT ID, NAME FROM NBP.NBP_ROLE WHERE ID = ?";
        List<Role> roles = jdbcTemplate.query(sql, (rs, rowNum) -> new Role(rs.getLong("ID"), rs.getString("NAME")), roleId);
        return roles.stream().findFirst();
    }

    public Optional<User> createUser(User user) {
        String sql = """
                INSERT INTO NBP.NBP_USER(FIRST_NAME, LAST_NAME, EMAIL, PASSWORD, USERNAME, ROLE_ID, BIRTH_DATE, ADDRESS_ID)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(sql,
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPassword(),
                user.getUsername(),
                user.getRole() != null ? user.getRole().getId() : null,
                user.getBirthDate() != null ? Date.valueOf(user.getBirthDate()) : null,
                null);

        return findByUsername(user.getUsername());
    }
}


