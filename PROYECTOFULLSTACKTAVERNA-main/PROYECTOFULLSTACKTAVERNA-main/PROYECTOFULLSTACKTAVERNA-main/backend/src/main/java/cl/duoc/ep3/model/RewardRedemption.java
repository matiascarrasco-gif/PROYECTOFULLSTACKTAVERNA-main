package cl.duoc.ep3.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reward_redemptions")
public class RewardRedemption {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Producto product;

    @Column(nullable = false)
    private int pointsCost;

    @Column(nullable = false)
    private boolean used;

    @Column(nullable = false)
    private Instant createdAt;

    protected RewardRedemption() {
    }

    public RewardRedemption(Account account, Producto product, int pointsCost) {
        this.id = UUID.randomUUID();
        this.account = account;
        this.product = product;
        this.pointsCost = pointsCost;
        this.used = false;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Account getAccount() { return account; }
    public Producto getProduct() { return product; }
    public int getPointsCost() { return pointsCost; }
    public boolean isUsed() { return used; }
    public void markUsed() { this.used = true; }
}
