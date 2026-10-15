package mn.demo.mini_bank.adapter.out.persistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import mn.demo.mini_bank.domain.model.Account;
import mn.demo.mini_bank.application.port.out.AccountRepositoryPort;
import mn.demo.mini_bank.adapter.out.persistence.entity.AccountJpaEntity;
import mn.demo.mini_bank.adapter.out.persistence.repository.AccountJpaRepository;

@Repository
public class AccountPersistenceAdapter implements AccountRepositoryPort {

    private final AccountJpaRepository repository;
    private final EntityManager entityManager;

    public AccountPersistenceAdapter(
            AccountJpaRepository repository,
            EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    public Account createAccount(UUID userId, BigDecimal balance) {
        Long sequence = repository.nextAccountNumber();
        String number = "DMB" + String.format("%012d", sequence);

        AccountJpaEntity entity = new AccountJpaEntity(
            UUID.randomUUID(), userId, number, balance
        );

        return toDomain(repository.save(entity));
    }

    @Override
    public List<Account> findByUserId(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtAsc(userId)
            .stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Account> findByAccountNumber(String number) {
        return repository.findByAccountNumber(number)
            .map(this::toDomain);
    }

    @Override
    public Optional<Account> lockById(UUID id) {
        return repository.lockById(id).map(entity -> {
            entityManager.refresh(entity);
            return toDomain(entity);
        });
    }

    @Override
    public void updateBalance(UUID id, BigDecimal balance) {
        AccountJpaEntity entity = repository.findById(id)
            .orElseThrow(() ->
                new IllegalArgumentException("Account not found"));

        entity.changeBalance(balance);
    }

    private Account toDomain(AccountJpaEntity entity) {
        return new Account(
            entity.getId(),
            entity.getUserId(),
            entity.getAccountNumber(),
            entity.getBalance(),
            entity.getCurrency(),
            entity.getStatus()
        );
    }

    @Override
    public List<Account> findByIds(Set<UUID> ids) {
        return repository.findAllById(ids)
                .stream()
                .map(this::toDomain)
                .toList();
    }

}
