package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.model.AuthLogAction;
import ba.unsa.etf.NBP.model.UserAuthLog;

import java.util.List;

/**
 * Service abstraction for recording and retrieving authentication events.
 * <p>
 * Implementations may persist logs (e.g. MongoDB) or act as a no-op when
 * auth logging is disabled (e.g. in tests).
 */
public interface AuthLogService {

    /**
     * Records an authentication event for a user.
     * <p>
     * Implementations should treat this as best-effort and avoid failing the
     * primary request flow.
     */
    void logEvent(Long userId, AuthLogAction action);

    /**
     * Returns authentication events for the given user, ordered newest-first.
     */
    List<UserAuthLog> getLogsForUser(Long userId);
}
