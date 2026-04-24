package ba.unsa.etf.NBP.controller;

import ba.unsa.etf.NBP.model.Notification;
import ba.unsa.etf.NBP.model.User;
import ba.unsa.etf.NBP.service.AuthService;
import ba.unsa.etf.NBP.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Notification endpoints under {@code /notifications}.
 * <p>
 * Covers CRUD plus per-user lookups, unread filtering, and marking as read.
 */
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthService authService;

    public NotificationController(NotificationService notificationService, AuthService authService) {
        this.notificationService = notificationService;
        this.authService = authService;
    }

    /**
     * Lists every notification.
     *
     * @return all notifications
     */
    @GetMapping
    public List<Notification> findAll() {
        return notificationService.findAll();
    }

    /**
     * Returns a single notification by ID.
     *
     * @param id notification ID
     * @return the notification, or {@code 404 Not Found}
     */
    @GetMapping("/{id}")
    public ResponseEntity<Notification> findById(@PathVariable Long id) {
        return notificationService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a new notification.
     *
     * @param notification notification payload
     * @return {@code 201 Created}
     */
    @PostMapping
    public ResponseEntity<Void> save(@RequestBody Notification notification) {
        notificationService.save(notification);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Updates an existing notification.
     *
     * @param id           notification ID
     * @param notification updated fields
     * @return {@code 200 OK}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody Notification notification) {
        notification.setId(id);
        notificationService.update(notification);
        return ResponseEntity.ok().build();
    }

    /**
     * Deletes a notification.
     *
     * @param id notification ID
     * @return {@code 204 No Content}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        notificationService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Marks a notification as read. Only the owner may call this.
     *
     * @param id notification ID
     * @return {@code 200 OK}
     * @throws ResponseStatusException 404 if not found, 403 if caller does not own the notification
     */
    @PutMapping("/{id}/read")
    @PreAuthorize(value = "isAuthenticated()")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        Notification notification = notificationService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!notification.getUserId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only mark your own notifications as read");
        }

        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Lists every notification for a user.
     *
     * @param userId user ID
     * @return notifications for that user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> findByUserId(@PathVariable Long userId) {
        List<Notification> notifications = notificationService.findByUserId(userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Lists only unread notifications for a user.
     *
     * @param userId user ID
     * @return unread notifications for that user
     */
    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<List<Notification>> findUnreadByUserId(@PathVariable Long userId) {
        List<Notification> notifications = notificationService.findUnreadByUserId(userId);
        return ResponseEntity.ok(notifications);
    }
}
