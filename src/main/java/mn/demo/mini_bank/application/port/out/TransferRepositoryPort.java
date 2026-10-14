package mn.demo.mini_bank.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import mn.demo.mini_bank.domain.model.Transfer;

public interface TransferRepositoryPort {

    Transfer save(Transfer transfer);

    Optional<Transfer> findByUserAndKey(UUID userId, UUID key);

    Optional<Transfer> findById(UUID id);

    SearchResult search(
            Set<UUID> myAccountIds,
            UUID accountFilterId,
            String currency,
            String keyword,
            Instant fromDate,
            Instant endExclusive,
            int page,
            int size
    );

    record SearchResult(
            List<Transfer> items,
            long totalElements,
            int page,
            int size
    ) {}
}
