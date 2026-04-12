package ba.unsa.etf.NBP.repository;

import ba.unsa.etf.NBP.model.Notification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class NotificationRepository {

    private final JdbcTemplate jdbcTemplate;

    public NotificationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Notification> rowMapper = (rs, rowNum) -> {
        Notification notification = new Notification();
        notification.setId(rs.getLong("ID"));
        notification.setUserId(rs.getLong("USER_ID"));
        notification.setTitle(rs.getString("TITLE"));
        notification.setMessage(rs.getString("MESSAGE"));
        notification.setRead(rs.getInt("IS_READ") == 1);
        notification.setCreatedAt(rs.getTimestamp("CREATED_AT") != null ? rs.getTimestamp("CREATED_AT").toLocalDateTime() : null);
        notification.setNotificationType(rs.getString("NOTIFICATION_TYPE"));
        notification.setCourseSessionId(rs.getObject("COURSE_SESSION_ID", Long.class));
        return notification;
    };

    public List<Notification> findAll() {
        String sql = "SELECT * FROM NBP_NOTIFICATION";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public Optional<Notification> findById(Long id) {
        String sql = "SELECT * FROM NBP_NOTIFICATION WHERE ID = ?";
        List<Notification> results = jdbcTemplate.query(sql, rowMapper, id);
        return results.stream().findFirst();
    }

    public void save(Notification notification) {
        String sql = "INSERT INTO NBP_NOTIFICATION (ID, USER_ID, TITLE, MESSAGE, IS_READ, CREATED_AT, NOTIFICATION_TYPE, COURSE_SESSION_ID) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                notification.getId(),
                notification.getUserId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead() ? 1 : 0,
                notification.getCreatedAt() != null ? java.sql.Timestamp.valueOf(notification.getCreatedAt()) : null,
                notification.getNotificationType(),
                notification.getCourseSessionId());
    }

    public void update(Notification notification) {
        String sql = "UPDATE NBP_NOTIFICATION SET USER_ID = ?, TITLE = ?, MESSAGE = ?, IS_READ = ?, " +
                     "CREATED_AT = ?, NOTIFICATION_TYPE = ?, COURSE_SESSION_ID = ? WHERE ID = ?";
        jdbcTemplate.update(sql,
                notification.getUserId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead() ? 1 : 0,
                notification.getCreatedAt() != null ? java.sql.Timestamp.valueOf(notification.getCreatedAt()) : null,
                notification.getNotificationType(),
                notification.getCourseSessionId(),
                notification.getId());
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM NBP_NOTIFICATION WHERE ID = ?";
        jdbcTemplate.update(sql, id);
    }

    public List<Notification> findByUserId(Long userId) {
        String sql = "SELECT * FROM NBP_NOTIFICATION WHERE USER_ID = ? ORDER BY CREATED_AT DESC";
        return jdbcTemplate.query(sql, rowMapper, userId);
    }

    public List<Notification> findUnreadByUserId(Long userId) {
        String sql = "SELECT * FROM NBP_NOTIFICATION WHERE USER_ID = ? AND IS_READ = 0 ORDER BY CREATED_AT DESC";
        return jdbcTemplate.query(sql, rowMapper, userId);
    }

    public void markAsRead(Long id) {
        String sql = "UPDATE NBP_NOTIFICATION SET IS_READ = 1 WHERE ID = ?";
        jdbcTemplate.update(sql, id);
    }
}
