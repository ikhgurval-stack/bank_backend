package mn.demo.mini_bank.application.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mn.demo.mini_bank.application.port.out.AccountRepositoryPort;
import mn.demo.mini_bank.application.port.out.TransferRepositoryPort;
import mn.demo.mini_bank.application.port.out.UserRepositoryPort;
import mn.demo.mini_bank.domain.model.Account;
import mn.demo.mini_bank.domain.model.Transfer;

@Service
public class TransferService {

    private final UserRepositoryPort users;
    private final AccountRepositoryPort accounts;
    private final TransferRepositoryPort transfers;
    private final CurrencyConversionService conversionService;

    public TransferService(
            UserRepositoryPort users,
            AccountRepositoryPort accounts,
            TransferRepositoryPort transfers,
            CurrencyConversionService conversionService) {

        this.users = users;
        this.accounts = accounts;
        this.transfers = transfers;
        this.conversionService = conversionService;
    }

    @Transactional
    public Transfer send(
            String username,
            String fromNumber,
            String toNumber,
            BigDecimal amount,
            String currency,
            String description,
            UUID key) {

        if (key == null) {
            throw new IllegalArgumentException(
                    "Idempotency key is required");
        }

        if (fromNumber == null || toNumber == null
                || fromNumber.equals(toNumber)) {
            throw new IllegalArgumentException(
                    "Invalid sender or recipient account");
        }

        if (description == null
                || description.isBlank()
                || description.length() > 255) {
            throw new IllegalArgumentException(
                    "Invalid description");
        }

        String note = description.trim();

        var conversion = conversionService.convert(
                amount, currency);

        BigDecimal settlementAmount =
                conversion.settlementAmount();

        String settlementCurrency =
                conversion.settlementCurrency();

        UUID userId = users.findByUsername(username)
                .orElseThrow(() ->
                    new IllegalStateException("User not found"))
                .id();

        Account source = accounts
                .findByAccountNumber(fromNumber)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "Sender account not found"));

        Account destination = accounts
                .findByAccountNumber(toNumber)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "Recipient account not found"));

        // Only the owner can transfer from an account.
        if (!source.userId().equals(userId)) {
            throw new AccessDeniedException(
                    "Not your account");
        }

        // Consistent lock ordering prevents deadlocks between opposite transfers.
        UUID firstId =
                source.id().compareTo(destination.id()) < 0
                    ? source.id()
                    : destination.id();

        UUID secondId = firstId.equals(source.id())
                ? destination.id()
                : source.id();

        Account first = accounts.lockById(firstId)
                .orElseThrow(() ->
                    new IllegalStateException(
                        "Account missing"));

        Account second = accounts.lockById(secondId)
                .orElseThrow(() ->
                    new IllegalStateException(
                        "Account missing"));

        Account from = first.id().equals(source.id())
                ? first : second;

        Account to = first.id().equals(source.id())
                ? second : first;

        // Check idempotency under the account locks before modifying balances.
        var previous = transfers.findByUserAndKey(
                userId, key);

        if (previous.isPresent()) {
            Transfer saved = previous.get();

            boolean sameRequest =
                    saved.fromAccountId().equals(from.id())
                    && saved.toAccountId().equals(to.id())
                    && saved.requestedAmount().compareTo(
                        conversion.requestedAmount()) == 0
                    && saved.requestedCurrency().equals(
                        conversion.requestedCurrency())
                    && saved.description().equals(note);

            if (!sameRequest) {
                throw new IllegalStateException(
                    "Idempotency key already used");
            }

            return saved;
        }

        if (!"ACTIVE".equals(from.status())
                || !"ACTIVE".equals(to.status())) {
            throw new IllegalStateException(
                    "Account is not active");
        }

        // All accounts are currently MNT accounts.
        if (!settlementCurrency.equals(from.currency())
                || !settlementCurrency.equals(to.currency())) {
            throw new IllegalArgumentException(
                    "Account currency mismatch");
        }

        // Check MNT balance after currency conversion.
        if (from.balance().compareTo(settlementAmount) < 0) {
            throw new IllegalStateException(
                    "Insufficient funds");
        }

        // Debit, credit and transfer persistence share the transaction.
        accounts.updateBalance(
                from.id(),
                from.balance().subtract(settlementAmount));

        accounts.updateBalance(
                to.id(),
                to.balance().add(settlementAmount));

        // Save both requested and settled amounts.
        Transfer transfer = new Transfer(
                UUID.randomUUID(),
                userId,
                from.id(),
                to.id(),

                settlementAmount,
                settlementCurrency,

                conversion.requestedAmount(),
                conversion.requestedCurrency(),
                conversion.exchangeRate(),

                note,
                "COMPLETED",
                key,
                Instant.now()
        );

        return transfers.save(transfer);
    }
}
