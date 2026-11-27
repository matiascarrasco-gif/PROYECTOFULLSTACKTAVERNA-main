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
@Table(name = "purchase_orders")
public class PurchaseOrder {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false)
    private long subtotal;

    @Column(nullable = false)
    private long discount;

    @Column(nullable = false)
    private long total;

    @Column(nullable = false)
    private int pointsEarned;

    @Column(nullable = false)
    private Instant createdAt;

    protected PurchaseOrder() {
    }

    public PurchaseOrder(UUID id, Account account, long subtotal, long discount, long total, int pointsEarned) {
        this.id = id;
        this.account = account;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
        this.pointsEarned = pointsEarned;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Account getAccount() { return account; }
    public long getSubtotal() { return subtotal; }
    public long getDiscount() { return discount; }
    public long getTotal() { return total; }
    public int getPointsEarned() { return pointsEarned; }
    public Instant getCreatedAt() { return createdAt; }
}
