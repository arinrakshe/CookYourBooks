package app.cookyourbooks.security;

import app.cookyourbooks.config.CookYourBooksProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final CookYourBooksProperties props;
    private final SecretKey key;

    public JwtService(CookYourBooksProperties props) {
        this.props = props;
        byte[] secret = Base64.getDecoder().decode(props.getJwt().getSecret());
        if (secret.length < 32) {
            throw new IllegalStateException(
                "cookyourbooks.jwt.secret must decode to at least 32 bytes (256 bits)");
        }
        this.key = Keys.hmacShaKeyFor(secret);
    }

    public String issueAccessToken(Long userId, String email) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.getJwt().getAccessTokenTtlMinutes() * 60);
        return Jwts.builder()
            .issuer(props.getJwt().getIssuer())
            .subject(String.valueOf(userId))
            .claims(Map.of("email", email))
            .issuedAt(Date.from(now))
            .expiration(Date.from(exp))
            .signWith(key, Jwts.SIG.HS256)
            .compact();
    }

    public ParsedJwt parse(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(props.getJwt().getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
            return new ParsedJwt(
                Long.parseLong(claims.getSubject()),
                claims.get("email", String.class),
                claims.getExpiration().toInstant());
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("Invalid or expired token", e);
        }
    }

    public record ParsedJwt(Long userId, String email, Instant expiresAt) {}

    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String msg, Throwable cause) { super(msg, cause); }
    }
}
