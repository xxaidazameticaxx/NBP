package ba.unsa.etf.NBP.security;

import ba.unsa.etf.NBP.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;

/**
 * JWT token issuance and validation.
 * <p>
 * Creates signed access and refresh tokens with configurable expiration,
 * parses and validates token claims, and extracts session and user identity.
 */
@Service
public class JwtTokenService {

    public static final String CLAIM_SESSION_ID = "sid";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_TYPE = "type";
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final String issuer;
    private final long accessTokenMinutes;
    private final long refreshTokenMinutes;

    public JwtTokenService(@Value("${app.auth.jwt-secret}") String secret,
                           @Value("${app.auth.jwt-issuer:nbp-app}") String issuer,
                           @Value("${app.auth.access-token-minutes:30}") long accessTokenMinutes,
                           @Value("${app.auth.refresh-token-minutes:10080}") long refreshTokenMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.accessTokenMinutes = accessTokenMinutes;
        this.refreshTokenMinutes = refreshTokenMinutes;
    }

    public String createAccessToken(User user, String sessionId) {
        return createToken(user, sessionId, TOKEN_TYPE_ACCESS, accessTokenMinutes);
    }

    public String createRefreshToken(User user, String sessionId) {
        return createToken(user, sessionId, TOKEN_TYPE_REFRESH, refreshTokenMinutes);
    }

    public Optional<Claims> parseClaims(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public boolean isRefreshToken(Claims claims) {
        return TOKEN_TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class));
    }

    public String extractSessionId(Claims claims) {
        return claims.get(CLAIM_SESSION_ID, String.class);
    }

    public Long extractUserId(Claims claims) {
        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(subject);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String createToken(User user, String sessionId, String tokenType, long ttlMinutes) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(ttlMinutes * 60);

        String roleName = user.getRole() != null ? user.getRole().getRoleName() : null;

        return Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(user.getId()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim(CLAIM_SESSION_ID, sessionId)
                .claim(CLAIM_ROLE, roleName)
                .claim(CLAIM_TYPE, tokenType)
                .signWith(key)
                .compact();
    }

    public LocalDateTime refreshTokenExpiryFromNow() {
        return LocalDateTime.now(ZoneOffset.UTC).plusMinutes(refreshTokenMinutes);
    }
}

