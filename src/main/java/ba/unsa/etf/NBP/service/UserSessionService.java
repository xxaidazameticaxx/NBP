package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.model.UserSession;
import ba.unsa.etf.NBP.repository.UserSessionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserSessionService {

    private final UserSessionRepository userSessionRepository;

    public UserSessionService(UserSessionRepository userSessionRepository) {
        this.userSessionRepository = userSessionRepository;
    }

    public List<UserSession> findAll() {
        return userSessionRepository.findAll();
    }

    public Optional<UserSession> findById(String sessionId) {
        return userSessionRepository.findById(sessionId);
    }

    public void save(UserSession userSession) {
        userSessionRepository.save(userSession);
    }

    public void update(UserSession userSession) {
        userSessionRepository.update(userSession);
    }

    public void deleteById(Long id) {
        userSessionRepository.deleteById(id);
    }

    public int deleteByUserId(Long userId) {
        return userSessionRepository.deleteByUserId(userId);
    }

    public List<UserSession> findByUserId(Long userId) {
        return userSessionRepository.findByUserId(userId);
    }

    public int deleteBySessionId(String sessionId) {
        return userSessionRepository.deleteBySessionId(sessionId);
    }

    public Optional<UserSession> findActiveBySessionId(String sessionId, LocalDateTime now) {
        return userSessionRepository.findActiveBySessionId(sessionId, now);
    }
}