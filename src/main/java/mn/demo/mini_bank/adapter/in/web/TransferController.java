package mn.demo.mini_bank.adapter.in.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import mn.demo.mini_bank.application.service.TransferService;
import mn.demo.mini_bank.application.service.TransferHistoryService;
import mn.demo.mini_bank.domain.model.Transfer;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final TransferService transferService;
    private final TransferHistoryService historyService;

    public TransferController(
            TransferService transferService,
            TransferHistoryService historyService) {

        this.transferService = transferService;
        this.historyService = historyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse send(
            Authentication auth,
            @RequestHeader("Idempotency-Key") UUID key,
            @Valid @RequestBody TransferRequest request) {

        Transfer transfer = transferService.send(
                auth.getName(),
                request.fromAccountNumber(),
                request.toAccountNumber(),
                request.amount(),
                request.currency(),
                request.description(),
                key
        );

        return new TransferResponse(
                transfer.id(),
                request.fromAccountNumber(),
                request.toAccountNumber(),

                // Actual MNT settlement
                transfer.amount().toPlainString(),
                transfer.currency(),

                // Original input and applied exchange rate
                transfer.requestedAmount().toPlainString(),
                transfer.requestedCurrency(),
                transfer.exchangeRate().toPlainString(),

                transfer.description(),
                transfer.status(),
                transfer.createdAt()
        );
    }

    @GetMapping
    public TransferHistoryService.HistoryPage history(
            Authentication auth,

            @RequestParam(required = false)
            String accountNumber,

            @RequestParam(required = false)
            String currency,

            @RequestParam(required = false)
            String keyword,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate fromDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate toDate,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size) {

        return historyService.list(
                auth.getName(),
                accountNumber,
                currency,
                keyword,
                fromDate,
                toDate,
                page,
                size
        );
    }

    @GetMapping("/{id}")
    public TransferHistoryService.HistoryItem detail(
            Authentication auth,
            @PathVariable UUID id) {

        return historyService.detail(
                auth.getName(), id);
    }

    public record TransferRequest(

        @NotBlank
        @Pattern(regexp = "DMB[0-9]{12}")
        String fromAccountNumber,

        @NotBlank
        @Pattern(regexp = "DMB[0-9]{12}")
        String toAccountNumber,

        @NotNull
        @DecimalMin("0.01")
        @Digits(integer = 17, fraction = 2)
        BigDecimal amount,

        @NotBlank
        @Pattern(regexp = "MNT|USD")
        String currency,

        @NotBlank
        @Size(max = 255)
        String description
    ) {}

    public record TransferResponse(
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
            Instant createdAt
    ) {}

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> badRequest(Exception ex) {
        return Map.of(
                "error", ex.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> notFound(Exception ex) {
        return Map.of(
                "error", "Transfer not found");
    }

    @ExceptionHandler({
        IllegalStateException.class,
        DataIntegrityViolationException.class
    })
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> conflict(Exception ex) {
        return Map.of(
                "error", "Transfer conflict");
    }
}
