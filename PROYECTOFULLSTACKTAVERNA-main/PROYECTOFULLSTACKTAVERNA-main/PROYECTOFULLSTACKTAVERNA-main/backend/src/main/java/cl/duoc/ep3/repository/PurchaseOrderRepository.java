package cl.duoc.ep3.repository;

import cl.duoc.ep3.model.PurchaseOrder;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID> {
    Optional<PurchaseOrder> findByIdAndAccountId(UUID id, Long accountId);
}
