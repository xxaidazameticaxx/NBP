package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.model.Notification;
import ba.unsa.etf.NBP.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

/**
 * CRUD and queries for user notifications.
 * <p>
 * Notifications inform students of status changes on their absence excuses,
 * attendance records, and administrative updates.
 */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Returns every notification.
     *
     * @return all notifications
     */
    public List<Notification> findAll() {
        return notificationRepository.findAll();
    }

    /**
     * Looks up a notification by ID.
     *
     * @param id notification ID
     * @return the notification, or {@link Optional#empty()} if missing
     */
    public Optional<Notification> findById(Long id) {
        return notificationRepository.findById(id);
    }

    /**
     * Inserts a new notification.
     *
     * @param notification notification to insert
     */
    public void save(Notification notification) {
        notificationRepository.save(notification);
    }

    /**
     * Updates a notification.
     *
     * @param notification notification with updated fields (ID required)
     */
    public void update(Notification notification) {
        notificationRepository.update(notification);
    }

    /**
     * Deletes a notification by ID.
     *
     * @param id notification ID
     */
    public void deleteById(Long id) {
        notificationRepository.deleteById(id);
    }

    /**
     * Returns all notifications for a user.
     *
     * @param userId user ID
     * @return notifications for that user
     */
    public List<Notification> findByUserId(Long userId) {
        return notificationRepository.findByUserId(userId);
    }

    /**
     * Returns unread notifications for a user.
     *
     * @param userId user ID
     * @return unread notifications for that user
     */
    public List<Notification> findUnreadByUserId(Long userId) {
        return notificationRepository.findUnreadByUserId(userId);
    }

    /**
     * Marks a notification as read.
     *
     * @param id notification ID
     */
    public void markAsRead(Long id) {
        notificationRepository.markAsRead(id);
    }
}
