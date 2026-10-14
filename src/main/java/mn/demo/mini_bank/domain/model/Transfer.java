package mn.demo.mini_bank.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Transfer(
    UUID id,
    UUID initiatedBy,
    UUID fromAccountId,
    UUID toAccountId,

    BigDecimal amount,
    String currency,

    BigDecimal requestedAmount,
    String requestedCurrency,
    BigDecimal exchangeRate,

    String description,
    String status,
    UUID idempotencyKey,
    Instant createdAt
) {

    // Compatibility constructor for existing MNT transfers
    public Transfer(
            UUID id,
            UUID initiatedBy,
            UUID fromAccountId,
            UUID toAccountId,
            BigDecimal amount,
            String currency,
            String description,
            String status,
            UUID idempotencyKey,
            Instant createdAt) {

        this(
            id,
            initiatedBy,
            fromAccountId,
            toAccountId,
            amount,
            currency,
            amount,
            currency,
            BigDecimal.ONE,
            description,
            status,
            idempotencyKey,
            createdAt
        );
    }
}
