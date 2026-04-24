package ba.unsa.etf.NBP.repository;

import ba.unsa.etf.NBP.model.Room;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JDBC repository for {@link Room} rows in {@code NBP_ROOM}.
 * <p>
 * Manages classroom and facility records with building associations
 * for course session scheduling and attendance location tracking.
 */
@Repository
public class RoomRepository {

    private final JdbcTemplate jdbcTemplate;

    public RoomRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Room> rowMapper = (rs, rowNum) -> {
        Room room = new Room();
        room.setId(rs.getLong("ID"));
        room.setName(rs.getString("NAME"));
        room.setBuilding(rs.getString("BUILDING"));
        return room;
    };

    public List<Room> findAll() {
        String sql = "SELECT * FROM NBP_ROOM";
        return jdbcTemplate.query(sql, rowMapper);
    }

    public Optional<Room> findById(Long id) {
        String sql = "SELECT * FROM NBP_ROOM WHERE ID = ?";
        List<Room> results = jdbcTemplate.query(sql, rowMapper, id);
        return results.stream().findFirst();
    }

    public void save(Room room) {
        String sql = "INSERT INTO NBP_ROOM (ID, NAME, BUILDING) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql,
                room.getId(),
                room.getName(),
                room.getBuilding());
    }

    public void update(Room room) {
        String sql = "UPDATE NBP_ROOM SET NAME = ?, BUILDING = ? WHERE ID = ?";
        jdbcTemplate.update(sql,
                room.getName(),
                room.getBuilding(),
                room.getId());
    }

    public void deleteById(Long id) {
        String sql = "DELETE FROM NBP_ROOM WHERE ID = ?";
        jdbcTemplate.update(sql, id);
    }
}
