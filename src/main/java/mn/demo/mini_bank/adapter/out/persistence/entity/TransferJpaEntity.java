package mn.demo.mini_bank.adapter.out.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;

import mn.demo.mini_bank.domain.model.Transfer;

@Entity
@Table(name = "transfers")
public class TransferJpaEntity {

    @Id
    private UUID id;

    @Column(name = "initiated_by")
    private UUID initiatedBy;

    @Column(name = "from_account_id")
    private UUID fromAccountId;

    @Column(name = "to_account_id")
    private UUID toAccountId;

    private BigDecimal amount;

    private String currency;

    @Column(name = "requested_amount",
            precision = 19, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "requested_currency",
            length = 3)
    private String requestedCurrency;

    @Column(name = "exchange_rate",
            precision = 19, scale = 2)
    private BigDecimal exchangeRate;

    private String description;

    private String status;

    @Column(name = "idempotency_key")
    private UUID idempotencyKey;

    @Column(name = "created_at")
    private Instant createdAt;

    protected TransferJpaEntity() {}

    public TransferJpaEntity(Transfer transfer) {
        this.id = transfer.id();
        this.initiatedBy = transfer.initiatedBy();
        this.fromAccountId = transfer.fromAccountId();
        this.toAccountId = transfer.toAccountId();

        this.amount = transfer.amount();
        this.currency = transfer.currency();

        this.requestedAmount = transfer.requestedAmount();
        this.requestedCurrency = transfer.requestedCurrency();
        this.exchangeRate = transfer.exchangeRate();

        this.description = transfer.description();
        this.status = transfer.status();
        this.idempotencyKey = transfer.idempotencyKey();
        this.createdAt = transfer.createdAt();
    }

    public Transfer toDomain() {

        // Support existing MNT records with nullable
        // requested_* columns from the transition period.
        BigDecimal originalAmount =
                requestedAmount != null
                        ? requestedAmount
                        : amount;

        String originalCurrency =
                requestedCurrency != null
                        ? requestedCurrency
                        : currency;

        BigDecimal appliedRate =
                exchangeRate != null
                        ? exchangeRate
                        : BigDecimal.ONE;

        return new Transfer(
            id,
            initiatedBy,
            fromAccountId,
            toAccountId,

            amount,
            currency,

            originalAmount,
            originalCurrency,
            appliedRate,

            description,
            status,
            idempotencyKey,
            createdAt
        );
    }
}
