package mn.demo.mini_bank.adapter.out.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import mn.demo.mini_bank.application.port.out.TransferRepositoryPort;
import mn.demo.mini_bank.adapter.out.persistence.entity.TransferJpaEntity;
import mn.demo.mini_bank.adapter.out.persistence.repository.TransferJpaRepository;
import mn.demo.mini_bank.domain.model.Transfer;

@Repository
public class TransferPersistenceAdapter implements TransferRepositoryPort {

    private final TransferJpaRepository repository;

    public TransferPersistenceAdapter(TransferJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Transfer save(Transfer transfer) {
        return repository.save(
            new TransferJpaEntity(transfer)
        ).toDomain();
    }

    @Override
    public Optional<Transfer> findByUserAndKey(
            UUID userId, UUID key) {
        return repository
            .findByInitiatedByAndIdempotencyKey(userId, key)
            .map(TransferJpaEntity::toDomain);
    }

    @Override
    public Optional<Transfer> findById(UUID id) {
        return repository.findById(id)
            .map(TransferJpaEntity::toDomain);
    }

    @Override
    public SearchResult search(
            Set<UUID> myAccountIds,
            UUID accountFilterId,
            String currency,
            String keyword,
            Instant fromDate,
            Instant endExclusive,
            int page,
            int size) {

        Specification<TransferJpaEntity> filter =
            (root, query, cb) -> {

                List<Predicate> conditions = new ArrayList<>();

                // Only transfers involving my accounts.
                conditions.add(cb.or(
                    root.get("fromAccountId").in(myAccountIds),
                    root.get("toAccountId").in(myAccountIds)
                ));

                if (accountFilterId != null) {
                    conditions.add(cb.or(
                        cb.equal(root.get("fromAccountId"), accountFilterId),
                        cb.equal(root.get("toAccountId"), accountFilterId)
                    ));
                }


if (currency != null) {
    conditions.add(
        cb.equal(
            cb.coalesce(
                root.<String>get("requestedCurrency"),
                root.<String>get("currency")
            ),
            currency
        )
    );
}


                if (keyword != null) {
                    String pattern = "%" +
                        keyword.toLowerCase(Locale.ROOT)
                            .replace("\\", "\\\\")
                            .replace("%", "\\%")
                            .replace("_", "\\_")
                        + "%";

                    conditions.add(cb.like(
                        cb.lower(root.get("description")),
                        pattern,
                        '\\'
                    ));
                }

                if (fromDate != null) {
                    conditions.add(cb.greaterThanOrEqualTo(
                        root.get("createdAt"), fromDate
                    ));
                }

                if (endExclusive != null) {
                    conditions.add(cb.lessThan(
                        root.get("createdAt"), endExclusive
                    ));
                }

                return cb.and(conditions.toArray(new Predicate[0]));
            };

        var pageable = PageRequest.of(
            page,
            size,
            Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")
            )
        );

        var result = repository.findAll(filter, pageable);

        return new SearchResult(
            result.getContent().stream()
                .map(TransferJpaEntity::toDomain)
                .toList(),
            result.getTotalElements(),
            page,
            size
        );
    }
}
