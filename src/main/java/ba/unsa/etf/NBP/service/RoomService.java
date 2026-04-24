package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.model.Room;
import ba.unsa.etf.NBP.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Thin service layer over {@link RoomRepository} providing CRUD for rooms.
 */
@Service
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    /**
     * Returns every room.
     *
     * @return all rooms
     */
    public List<Room> findAll() {
        return roomRepository.findAll();
    }

    /**
     * Looks up a room by ID.
     *
     * @param id room ID
     * @return the room, or {@link Optional#empty()} if missing
     */
    public Optional<Room> findById(Long id) {
        return roomRepository.findById(id);
    }

    /**
     * Inserts a new room row.
     *
     * @param room room to insert
     */
    public void save(Room room) {
        roomRepository.save(room);
    }

    /**
     * Updates a room row.
     *
     * @param room room with updated fields (ID required)
     */
    public void update(Room room) {
        roomRepository.update(room);
    }

    /**
     * Deletes a room by ID.
     *
     * @param id room ID
     */
    public void deleteById(Long id) {
        roomRepository.deleteById(id);
    }
}
