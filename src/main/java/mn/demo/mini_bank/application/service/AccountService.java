package mn.demo.mini_bank.application.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mn.demo.mini_bank.application.port.out.AccountRepositoryPort;
import mn.demo.mini_bank.application.port.out.UserRepositoryPort;
import mn.demo.mini_bank.domain.model.Account;
import java.util.NoSuchElementException;
@Service
public class AccountService {

    private final UserRepositoryPort users;
    private final AccountRepositoryPort accounts;

    public AccountService(
            UserRepositoryPort users,
            AccountRepositoryPort accounts) {
        this.users = users;
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public List<Account> getMyAccounts(String username) {
        return accounts.findByUserId(getUserId(username));
    }

    @Transactional
    public Account openAccount(String username) {
        return accounts.createAccount(
            getUserId(username),
            BigDecimal.ZERO
        );
    }
    
@Transactional(readOnly = true)
public RecipientInfo findRecipient(String accountNumber) {

    if (accountNumber == null
            || !accountNumber.matches("DMB[0-9]{12}")) {
        throw new IllegalArgumentException(
                "Invalid account number");
    }

    Account account = accounts.findByAccountNumber(accountNumber)
        .filter(a -> "ACTIVE".equals(a.status()))
        .orElseThrow(() ->
            new NoSuchElementException("Account not found"));

    var owner = users.findById(account.userId())
        .orElseThrow(() ->
            new NoSuchElementException("Account owner not found"));

    String displayName = owner.fullName();

    if (displayName == null || displayName.isBlank()) {
        displayName = owner.username();
    }

    return new RecipientInfo(
        account.accountNumber(),
        displayName,
        account.currency()
    );
}

public record RecipientInfo(
    String accountNumber,
    String displayName,
    String currency
) {}

    private UUID getUserId(String username) {
        return users.findByUsername(username)
            .orElseThrow(() ->
                new IllegalStateException("User not found"))
            .id();
    }
}
