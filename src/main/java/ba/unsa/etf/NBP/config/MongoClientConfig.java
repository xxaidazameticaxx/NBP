package ba.unsa.etf.NBP.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

import java.util.concurrent.TimeUnit;

/**
 * Ensures MongoDB connection settings are taken from environment variables.
 * <p>
 * In some environments, configuration binding may fall back to defaults (e.g. localhost) or the
 * database name may default to {@code test}. This configuration forces the client URI and the
 * database name to be derived from {@code SPRING_DATA_MONGODB_URI} and
 * {@code SPRING_DATA_MONGODB_DATABASE} (with safe fallbacks).
 */
@Configuration
@ConditionalOnProperty(name = "app.auth-logging.enabled", havingValue = "true", matchIfMissing = true)
public class MongoClientConfig {

    @Bean
    @Primary
    public MongoClient mongoClient() {
        String uri = System.getenv("SPRING_DATA_MONGODB_URI");
        if (uri == null || uri.isBlank()) {
            uri = System.getenv("MONGODB_URI");
        }
        if (uri == null || uri.isBlank()) {
            uri = "mongodb://localhost:27017/nbp_logs";
        }

        ConnectionString connectionString = new ConnectionString(uri);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .applyToClusterSettings(builder -> builder.serverSelectionTimeout(2, TimeUnit.SECONDS))
                .applyToSocketSettings(builder -> builder.connectTimeout(2, TimeUnit.SECONDS))
                .build();

        return MongoClients.create(settings);
    }

    @Bean
    @Primary
    public MongoDatabaseFactory mongoDatabaseFactory(MongoClient mongoClient) {
        String databaseName = System.getenv("SPRING_DATA_MONGODB_DATABASE");
        if (databaseName == null || databaseName.isBlank()) {
            databaseName = "nbp_logs";
        }
        return new SimpleMongoClientDatabaseFactory(mongoClient, databaseName);
    }
}
