package mn.demo.mini_bank.adapter.out.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mn.demo.mini_bank.adapter.out.persistence.entity.AccountJpaEntity;

public interface AccountJpaRepository
        extends JpaRepository<AccountJpaEntity, UUID> {

    @Query(value = "SELECT nextval('account_number_seq')",
           nativeQuery = true)
    Long nextAccountNumber();

    List<AccountJpaEntity> findByUserIdOrderByCreatedAtAsc(UUID userId);

    Optional<AccountJpaEntity> findByAccountNumber(String number);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AccountJpaEntity a WHERE a.id = :id")
    Optional<AccountJpaEntity> lockById(@Param("id") UUID id);
}
