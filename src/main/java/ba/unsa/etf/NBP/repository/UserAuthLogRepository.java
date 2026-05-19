package ba.unsa.etf.NBP.repository;

import ba.unsa.etf.NBP.model.UserAuthLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * MongoDB repository for {@link UserAuthLog} documents.
 */
public interface UserAuthLogRepository extends MongoRepository<UserAuthLog, String> {

    /**
     * Finds auth logs for a user ordered newest-first.
     */
    List<UserAuthLog> findByUserIdOrderByTimestampDesc(Long userId);
}
