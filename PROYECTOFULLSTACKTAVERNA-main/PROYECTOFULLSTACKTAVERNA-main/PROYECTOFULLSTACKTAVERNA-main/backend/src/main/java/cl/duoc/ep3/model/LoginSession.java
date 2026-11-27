package cl.duoc.ep3.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "login_sessions")
public class LoginSession {

    @Id
    @Column(length = 36)
    private String tokenId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant revokedAt;

    protected LoginSession() {
    }

    public LoginSession(String tokenId, Account account, Instant expiresAt) {
        this.tokenId = tokenId;
        this.account = account;
        this.expiresAt = expiresAt;
    }

    public String getTokenId() { return tokenId; }
    public Account getAccount() { return account; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public void revoke(Instant time) { this.revokedAt = time; }
    public boolean isActiveAt(Instant time) {
        return revokedAt == null && expiresAt.isAfter(time);
    }
}
