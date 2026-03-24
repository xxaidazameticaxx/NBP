package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.model.Room;
import ba.unsa.etf.NBP.repository.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public List<Room> findAll() {
        return roomRepository.findAll();
    }

    public Optional<Room> findById(Long id) {
        return roomRepository.findById(id);
    }

    public void save(Room room) {
        roomRepository.save(room);
    }

    public void update(Room room) {
        roomRepository.update(room);
    }

    public void deleteById(Long id) {
        roomRepository.deleteById(id);
    }
}