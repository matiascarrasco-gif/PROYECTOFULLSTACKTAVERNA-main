package cl.duoc.ep3.repository;

import cl.duoc.ep3.model.Account;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    @Query(value = "select * from accounts where id = :id for update", nativeQuery = true)
    Optional<Account> findByIdForUpdate(@Param("id") Long id);
}
