package cl.duoc.ep3.service;

import cl.duoc.ep3.model.Account;
import cl.duoc.ep3.model.LoginSession;
import cl.duoc.ep3.repository.LoginSessionRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final Duration tokenLifetime;
    private final LoginSessionRepository sessionRepository;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration:PT15M}") Duration tokenLifetime,
            LoginSessionRepository sessionRepository) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.tokenLifetime = tokenLifetime;
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public AuthToken createToken(Account account) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(tokenLifetime);
        String tokenId = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .id(tokenId)
                .subject(account.getEmail())
                .claim("role", account.getRole().name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
        // Guardar el jti permite revocar el token antes de que expire.
        sessionRepository.save(new LoginSession(tokenId, account, expiresAt));
        return new AuthToken(token, expiresAt);
    }

    public Optional<AuthenticatedSession> authenticate(String token) {
        Jws<Claims> parsed = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
        String tokenId = parsed.getPayload().getId();
        Instant now = Instant.now();
        // Además de validar la firma, se exige que la sesión siga activa en la base.
        return sessionRepository.findByTokenIdAndRevokedAtIsNullAndExpiresAtAfter(tokenId, now)
                .filter(session -> session.getAccount().getEmail().equalsIgnoreCase(parsed.getPayload().getSubject()))
                .map(session -> new AuthenticatedSession(session, parsed.getPayload().getExpiration().toInstant()));
    }

    @Transactional
    public void revoke(String token) {
        Jws<Claims> parsed = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
        sessionRepository.findById(parsed.getPayload().getId()).ifPresent(session -> session.revoke(Instant.now()));
    }

    public record AuthToken(String value, Instant expiresAt) { }
    public record AuthenticatedSession(LoginSession session, Instant tokenExpiresAt) { }
}
