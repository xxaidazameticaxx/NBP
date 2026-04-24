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

## Authentication

The API uses JWT (JSON Web Token) authentication. All requests require a Bearer token except `/auth/login` and `/auth/refresh`.

### Getting a Token

1. Post your credentials to `/auth/login`:
   ```bash
   curl -X POST http://localhost:9000/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"user","password":"<password>"}'
   ```

2. The response contains your JWT token. Use it for authenticated requests:
   ```bash
   curl -X GET http://localhost:9000/courses \
     -H "Authorization: Bearer <your-jwt-token>"
   ```

### Interactive Testing with Swagger UI

Visit `http://localhost:9000/swagger-ui.html` to test the API interactively.

1. Click **Authorize** button
2. Paste your JWT token in the **Value** field: `Bearer <your-jwt-token>`
3. Click **Authorize**, then explore and test endpoints

### Default Login

On first startup, Spring Security provides auto-generated credentials:

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

## API Documentation

### Javadoc

Complete API documentation is available via Javadoc. To generate it locally:

```bash
./mvnw javadoc:javadoc
```

The generated HTML documentation will be available at `target/reports/apidocs/index.html`.
