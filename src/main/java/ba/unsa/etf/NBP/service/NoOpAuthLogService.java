package ba.unsa.etf.NBP.service;

import ba.unsa.etf.NBP.model.AuthLogAction;
import ba.unsa.etf.NBP.model.UserAuthLog;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * No-op {@link AuthLogService} used when auth logging is disabled.
 */
@Service
@ConditionalOnProperty(name = "app.auth-logging.enabled", havingValue = "false")
public class NoOpAuthLogService implements AuthLogService {

    @Override
    public void logEvent(Long userId, AuthLogAction action) {
        // intentionally no-op
    }

    @Override
    public List<UserAuthLog> getLogsForUser(Long userId) {
        return Collections.emptyList();
    }
}
