package mn.demo.mini_bank.adapter.in.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import mn.demo.mini_bank.application.service.AccountService;
import mn.demo.mini_bank.domain.model.Account;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public List<AccountResponse> myAccounts(Authentication auth) {
        return service.getMyAccounts(auth.getName())
            .stream()
            .map(AccountResponse::from)
            .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(Authentication auth) {
        return AccountResponse.from(
            service.openAccount(auth.getName())
        );
    }

    @GetMapping("/recipient")
    public AccountService.RecipientInfo recipient(
            @RequestParam String accountNumber) {
        return service.findRecipient(accountNumber);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> badRequest() {
        return Map.of("error", "Invalid account number");
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> notFound() {
        return Map.of("error", "Recipient account not found");
    }

    public record AccountResponse(
        String accountNumber,
        BigDecimal balance,
        String currency,
        String status
    ) {
        static AccountResponse from(Account account) {
            return new AccountResponse(
                account.accountNumber(),
                account.balance(),
                account.currency(),
                account.status()
            );
        }
    }
}
