# NBP

Spring Boot application connecting to the shared ETF Oracle database.

## Configuration

1. Copy the example env file:
   ```bash
   cp .env.example .env
   ```

2. Fill in your values in `.env`:

   | Variable | Description |
   |---|---|
   | `DB_URL` | Oracle JDBC URL (`jdbc:oracle:thin:@<host>:<port>:<sid>`) |
   | `DB_USERNAME` | Database username |
   | `DB_PASSWORD` | Database password |
   | `JWT_SECRET` | JWT signing secret (required, use at least 32 characters) |

## Login

Spring Security is active. Use the auto-generated credentials printed in the console on startup:

- **Username:** `user`
- **Password:** printed in the logs, e.g.:
  ```
  Using generated security password: 3f4a1b2c-xxxx-xxxx-xxxx-xxxxxxxxxxxx
  ```

## Running

### With Docker

```bash
# Build the jar
./mvnw package -DskipTests

# Start the app
docker compose up
```

The app will be available at `http://localhost:9000`.

### Without Docker

```bash
export DB_URL=jdbc:oracle:thin:@<host>:1521:<sid>
export DB_USERNAME=your_username
export DB_PASSWORD=your_password
export JWT_SECRET=very_long_random_secret_at_least_32_characters

./mvnw spring-boot:run
```