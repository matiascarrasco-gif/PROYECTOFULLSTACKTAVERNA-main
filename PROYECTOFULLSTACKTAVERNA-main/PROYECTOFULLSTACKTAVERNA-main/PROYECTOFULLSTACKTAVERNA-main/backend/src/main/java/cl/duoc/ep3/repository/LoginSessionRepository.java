package cl.duoc.ep3.repository;

import cl.duoc.ep3.model.LoginSession;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginSessionRepository extends JpaRepository<LoginSession, String> {
    Optional<LoginSession> findByTokenIdAndRevokedAtIsNullAndExpiresAtAfter(String tokenId, Instant now);
}
