package ba.unsa.etf.NBP.controller;

import ba.unsa.etf.NBP.model.Room;
import ba.unsa.etf.NBP.service.RoomService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD endpoints for classrooms under {@code /rooms}.
 */
@RestController
@RequestMapping("/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    /**
     * Lists every room.
     *
     * @return all rooms
     */
    @GetMapping
    public List<Room> findAll() {
        return roomService.findAll();
    }

    /**
     * Returns a single room by ID.
     *
     * @param id room ID
     * @return the room, or {@code 404 Not Found}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Room> findById(@PathVariable Long id) {
        return roomService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a new room.
     *
     * @param room room payload
     * @return {@code 201 Created}
     */
    @PostMapping
    public ResponseEntity<Void> save(@RequestBody Room room) {
        roomService.save(room);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Updates an existing room.
     *
     * @param id   room ID
     * @param room updated fields
     * @return {@code 200 OK}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody Room room) {
        room.setId(id);
        roomService.update(room);
        return ResponseEntity.ok().build();
    }

    /**
     * Deletes a room.
     *
     * @param id room ID
     * @return {@code 204 No Content}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        roomService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
