package mn.demo.mini_bank.application.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mn.demo.mini_bank.application.port.out.AccountRepositoryPort;
import mn.demo.mini_bank.application.port.out.TransferRepositoryPort;
import mn.demo.mini_bank.application.port.out.UserRepositoryPort;
import mn.demo.mini_bank.domain.model.Account;
import mn.demo.mini_bank.domain.model.Transfer;

@Service
public class TransferHistoryService {

    private final UserRepositoryPort users;
    private final AccountRepositoryPort accounts;
    private final TransferRepositoryPort transfers;

    public TransferHistoryService(
            UserRepositoryPort users,
            AccountRepositoryPort accounts,
            TransferRepositoryPort transfers) {

        this.users = users;
        this.accounts = accounts;
        this.transfers = transfers;
    }

    @Transactional(readOnly = true)
    public HistoryPage list(
            String username,
            String accountNumber,
            String currency,
            String keyword,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size) {

        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Invalid pagination");
        }

        if (fromDate != null && toDate != null
                && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException(
                    "Invalid date range");
        }

        // Filter by originally requested currency.
        String currencyFilter =
                currency == null || currency.isBlank()
                    ? null
                    : currency.trim();

        if (currencyFilter != null
                && !"MNT".equals(currencyFilter)
                && !"USD".equals(currencyFilter)) {
            throw new IllegalArgumentException(
                    "Unsupported currency");
        }

        String searchText =
                keyword == null ? null : keyword.trim();

        if (searchText != null && searchText.isEmpty()) {
            searchText = null;
        }

        if (searchText != null && searchText.length() > 100) {
            throw new IllegalArgumentException(
                    "Keyword too long");
        }

        Set<UUID> myIds = myAccountIds(username);

        if (myIds.isEmpty()) {
            return new HistoryPage(
                    List.of(), 0, page, size, 0);
        }

        UUID filterId = null;

        if (accountNumber != null
                && !accountNumber.isBlank()) {

            var account = accounts.findByAccountNumber(
                    accountNumber.trim());

            if (account.isEmpty()) {
                return new HistoryPage(
                        List.of(), 0, page, size, 0);
            }

            filterId = account.get().id();
        }

        Instant start = fromDate == null
                ? null
                : fromDate.atStartOfDay(ZoneOffset.UTC)
                    .toInstant();

        Instant end = toDate == null
                ? null
                : toDate.plusDays(1)
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant();

        var result = transfers.search(
                myIds,
                filterId,
                currencyFilter,
                searchText,
                start,
                end,
                page,
                size
        );

        Map<UUID, String> numbers =
                accountNumbers(result.items());

        List<HistoryItem> items =
                result.items().stream()
                    .map(t -> toItem(t, myIds, numbers))
                    .toList();

        int totalPages = (int) (
                (result.totalElements() + size - 1) / size
        );

        return new HistoryPage(
                items,
                result.totalElements(),
                page,
                size,
                totalPages
        );
    }

    @Transactional(readOnly = true)
    public HistoryItem detail(String username, UUID id) {

        Set<UUID> myIds = myAccountIds(username);

        Transfer transfer = transfers.findById(id)
                .orElseThrow(() ->
                    new NoSuchElementException(
                        "Transfer not found"));

        if (!myIds.contains(transfer.fromAccountId())
                && !myIds.contains(transfer.toAccountId())) {
            throw new NoSuchElementException(
                    "Transfer not found");
        }

        return toItem(
                transfer,
                myIds,
                accountNumbers(List.of(transfer))
        );
    }

    private Set<UUID> myAccountIds(String username) {

        UUID userId = users.findByUsername(username)
                .orElseThrow(() ->
                    new NoSuchElementException(
                        "User not found"))
                .id();

        return accounts.findByUserId(userId).stream()
                .map(Account::id)
                .collect(Collectors.toSet());
    }

    private Map<UUID, String> accountNumbers(
            List<Transfer> items) {

        Set<UUID> ids = new HashSet<>();

        for (Transfer transfer : items) {
            ids.add(transfer.fromAccountId());
            ids.add(transfer.toAccountId());
        }

        return accounts.findByIds(ids).stream()
                .collect(Collectors.toMap(
                    Account::id,
                    Account::accountNumber
                ));
    }

    private HistoryItem toItem(
            Transfer transfer,
            Set<UUID> myIds,
            Map<UUID, String> numbers) {

        boolean sent =
                myIds.contains(transfer.fromAccountId());

        boolean received =
                myIds.contains(transfer.toAccountId());

        String direction =
                sent && received ? "INTERNAL"
                : sent ? "OUTGOING"
                : "INCOMING";

        return new HistoryItem(
                transfer.id(),
                numbers.get(transfer.fromAccountId()),
                numbers.get(transfer.toAccountId()),

                // Actual money movement in MNT
                transfer.amount().toPlainString(),
                transfer.currency(),

                // Original amount entered by the user
                transfer.requestedAmount().toPlainString(),
                transfer.requestedCurrency(),
                transfer.exchangeRate().toPlainString(),

                transfer.description(),
                transfer.status(),
                direction,
                transfer.createdAt()
        );
    }

    public record HistoryItem(
            UUID id,
            String fromAccountNumber,
            String toAccountNumber,

            String amount,
            String currency,

            String requestedAmount,
            String requestedCurrency,
            String exchangeRate,

            String description,
            String status,
            String direction,
            Instant createdAt
    ) {}

    public record HistoryPage(
            List<HistoryItem> items,
            long totalElements,
            int page,
            int size,
            int totalPages
    ) {}
}
