package mn.demo.mini_bank.adapter.out.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import mn.demo.mini_bank.adapter.out.persistence.entity.TransferJpaEntity;

public interface TransferJpaRepository
        extends JpaRepository<TransferJpaEntity, UUID>,
                JpaSpecificationExecutor<TransferJpaEntity> {

    Optional<TransferJpaEntity> findByInitiatedByAndIdempotencyKey(
            UUID initiatedBy,
            UUID idempotencyKey
    );
}
