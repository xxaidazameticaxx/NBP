package ba.unsa.etf.NBP.repository;

import ba.unsa.etf.NBP.model.UserSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JDBC repository for {@link UserSession} rows in {@code NBP_USER_SESSION}.
 * <p>
 * Tracks JWT session tokens with expiration, supporting active session lookups
 * and user-specific session management for token validation.
 */
@Repository
public class UserSessionRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserSessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<UserSession> rowMapper = (rs, rowNum) -> {
        UserSession userSession = new UserSession();
        userSession.setSessionId(rs.getString("SESSION_ID"));
        userSession.setUserId(rs.getLong("USER_ID"));
        userSession.setCreatedAt(rs.getTimestamp("CREATED_AT") != null ? rs.getTimestamp("CREATED_AT").toLocalDateTime() : null);
        userSession.setExpiresAt(rs.getTimestamp("EXPIRES_AT") != null ? rs.getTimestamp("EXPIRES_AT").toLocalDateTime() : null);
        return userSession;
    };

    public List<UserSession> findAll() {
        String sql = "SELECT * FROM NBP_USER_SESSION";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public Optional<UserSession> findById(String sessionId) {
        String sql = "SELECT * FROM NBP_USER_SESSION WHERE SESSION_ID = ?";
        List<UserSession> results = jdbcTemplate.query(sql, rowMapper, sessionId);
        return results.stream().findFirst();
    }

    public void save(UserSession userSession) {
        String sql = "INSERT INTO NBP_USER_SESSION (SESSION_ID, USER_ID, CREATED_AT, EXPIRES_AT) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                userSession.getSessionId(),
                userSession.getUserId(),
                userSession.getCreatedAt() != null ? java.sql.Timestamp.valueOf(userSession.getCreatedAt()) : null,
                userSession.getExpiresAt() != null ? java.sql.Timestamp.valueOf(userSession.getExpiresAt()) : null);
    }

    public void update(UserSession userSession) {
        String sql = "UPDATE NBP_USER_SESSION SET USER_ID = ?, CREATED_AT = ?, EXPIRES_AT = ? WHERE SESSION_ID = ?";
        jdbcTemplate.update(sql,
                userSession.getUserId(),
                userSession.getCreatedAt() != null ? java.sql.Timestamp.valueOf(userSession.getCreatedAt()) : null,
                userSession.getExpiresAt() != null ? java.sql.Timestamp.valueOf(userSession.getExpiresAt()) : null,
                userSession.getSessionId());
    }
    
        public void deleteById(Long id) {
        String sql = "DELETE FROM NBP_USER_SESSION WHERE USER_ID = ?";
        jdbcTemplate.update(sql, id);
    }

    public List<UserSession> findByUserId(Long userId) {
        String sql = "SELECT * FROM NBP_USER_SESSION WHERE USER_ID = ?";
        return jdbcTemplate.query(sql, rowMapper, userId);
    }

    public int deleteBySessionId(String sessionId) {
        String sql = "DELETE FROM NBP_USER_SESSION WHERE SESSION_ID = ?";
        return jdbcTemplate.update(sql, sessionId);
    }

    public Optional<UserSession> findActiveBySessionId(String sessionId, LocalDateTime now) {
        String sql = "SELECT * FROM NBP_USER_SESSION WHERE SESSION_ID = ? AND EXPIRES_AT > ?";
        List<UserSession> results = jdbcTemplate.query(sql, rowMapper, sessionId, java.sql.Timestamp.valueOf(now));
        return results.stream().findFirst();
    }
}
