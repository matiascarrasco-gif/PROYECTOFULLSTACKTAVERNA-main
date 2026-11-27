package cl.duoc.ep3.repository;

import cl.duoc.ep3.model.RewardRedemption;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RewardRedemptionRepository extends JpaRepository<RewardRedemption, UUID> {

    @Query(value = "select * from reward_redemptions "
            + "where id = :id and account_id = :accountId for update", nativeQuery = true)
    Optional<RewardRedemption> findByIdAndAccountIdForUpdate(
            @Param("id") UUID id,
            @Param("accountId") Long accountId
    );
}
