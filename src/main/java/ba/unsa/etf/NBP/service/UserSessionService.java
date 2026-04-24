package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.model.UserSession;
import ba.unsa.etf.NBP.repository.UserSessionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * CRUD for JWT session tokens.
 * <p>
 * Each login creates a {@code UserSession} row pairing a unique session ID with the user,
 * stored with expiration for access and refresh token validation.
 */
@Service
public class UserSessionService {

    private final UserSessionRepository userSessionRepository;

    public UserSessionService(UserSessionRepository userSessionRepository) {
        this.userSessionRepository = userSessionRepository;
    }

    /**
     * Returns every user session.
     *
     * @return all sessions
     */
    public List<UserSession> findAll() {
        return userSessionRepository.findAll();
    }

    /**
     * Looks up a user session by session ID.
     *
     * @param sessionId session ID (UUID)
     * @return the session, or {@link Optional#empty()} if missing
     */
    public Optional<UserSession> findById(String sessionId) {
        return userSessionRepository.findById(sessionId);
    }

    /**
     * Inserts a new user session.
     *
     * @param userSession session to insert
     */
    public void save(UserSession userSession) {
        userSessionRepository.save(userSession);
    }

    /**
     * Updates a user session.
     *
     * @param userSession session with updated fields (ID required)
     */
    public void update(UserSession userSession) {
        userSessionRepository.update(userSession);
    }

    /**
     * Deletes a session by its auto-increment ID.
     *
     * @param id auto-increment ID
     */
    public void deleteById(Long id) {
        userSessionRepository.deleteById(id);
    }

    /**
     * Deletes all sessions for a user.
     *
     * @param userId user ID
     */
    public void deleteByUserId(Long userId) {
        userSessionRepository.deleteById(userId);
    }

    /**
     * Returns all sessions for a user.
     *
     * @param userId user ID
     * @return sessions for that user
     */
    public List<UserSession> findByUserId(Long userId) {
        return userSessionRepository.findByUserId(userId);
    }

    /**
     * Deletes a session by session ID.
     *
     * @param sessionId session ID (UUID)
     * @return number of rows deleted
     */
    public int deleteBySessionId(String sessionId) {
        return userSessionRepository.deleteBySessionId(sessionId);
    }

    /**
     * Finds an active (non-expired) session by session ID.
     *
     * @param sessionId session ID (UUID)
     * @param now the current time
     * @return the session if active, or {@link Optional#empty()} if expired or missing
     */
    public Optional<UserSession> findActiveBySessionId(String sessionId, LocalDateTime now) {
        return userSessionRepository.findActiveBySessionId(sessionId, now);
    }
}