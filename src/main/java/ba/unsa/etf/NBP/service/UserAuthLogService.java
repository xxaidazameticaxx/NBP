package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.model.AuthLogAction;
import ba.unsa.etf.NBP.model.UserAuthLog;
import ba.unsa.etf.NBP.repository.UserAuthLogRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * MongoDB-backed implementation of {@link AuthLogService}.
 * <p>
 * Writes are performed asynchronously and treated as best-effort.
 */
@Service
@ConditionalOnProperty(name = "app.auth-logging.enabled", havingValue = "true", matchIfMissing = true)
public class UserAuthLogService implements AuthLogService {

    private static final Logger log = LoggerFactory.getLogger(UserAuthLogService.class);

    private final UserAuthLogRepository userAuthLogRepository;

    public UserAuthLogService(UserAuthLogRepository userAuthLogRepository) {
        this.userAuthLogRepository = userAuthLogRepository;
    }

    @Override
    @Async
    public void logEvent(Long userId, AuthLogAction action) {
        if (userId == null || action == null) {
            return;
        }
        try {
            userAuthLogRepository.save(new UserAuthLog(userId, Instant.now(), action));
        } catch (Exception ex) {
            log.warn("Auth log write failed (userId={}, action={})", userId, action, ex);
        }
    }

    @Override
    public List<UserAuthLog> getLogsForUser(Long userId) {
        try {
            return userAuthLogRepository.findByUserIdOrderByTimestampDesc(userId);
        } catch (Exception ex) {
            log.warn("Auth log read failed (userId={})", userId, ex);
            return Collections.emptyList();
        }
    }
}
