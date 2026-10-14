package mn.demo.mini_bank.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record Account(
    UUID id,
    UUID userId,
    String accountNumber,
    BigDecimal balance,
    String currency,
    String status
) {}
