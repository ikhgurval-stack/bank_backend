package mn.demo.mini_bank.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class AccountJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "account_number", nullable = false, unique = true)
    private String accountNumber;

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AccountJpaEntity() {}

    public AccountJpaEntity(
            UUID id,
            UUID userId,
            String accountNumber,
            BigDecimal balance) {
        this.id = id;
        this.userId = userId;
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.currency = "MNT";
        this.status = "ACTIVE";
        this.createdAt = Instant.now();
    }

    public String getAccountNumber() {
        return accountNumber;
    }
    
public UUID getId() {
    return id;
}

public UUID getUserId() {
    return userId;
}

public BigDecimal getBalance() {
    return balance;
}

public String getCurrency() {
    return currency;
}

public String getStatus() {
    return status;
}

public void changeBalance(BigDecimal newBalance) {
    if (newBalance.signum() < 0) {
        throw new IllegalArgumentException("Negative balance");
    }
    this.balance = newBalance;
}


}
