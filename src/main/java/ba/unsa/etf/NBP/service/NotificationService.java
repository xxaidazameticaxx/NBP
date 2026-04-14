package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.model.Notification;
import ba.unsa.etf.NBP.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public List<Notification> findAll() {
        return notificationRepository.findAll();
    }

    public Optional<Notification> findById(Long id) {
        return notificationRepository.findById(id);
    }

    public void save(Notification notification) {
        notificationRepository.save(notification);
    }

    public void update(Notification notification) {
        notificationRepository.update(notification);
    }

    public void deleteById(Long id) {
        notificationRepository.deleteById(id);
    }

    public List<Notification> findByUserId(Long userId) {
        return notificationRepository.findByUserId(userId);
    }

    public List<Notification> findUnreadByUserId(Long userId) {
        return notificationRepository.findUnreadByUserId(userId);
    }

    public void markAsRead(Long id) {
        notificationRepository.markAsRead(id);
    }
}
