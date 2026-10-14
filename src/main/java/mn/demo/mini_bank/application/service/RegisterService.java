package mn.demo.mini_bank.application.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mn.demo.mini_bank.application.port.out.AccountRepositoryPort;
import mn.demo.mini_bank.application.port.out.UserRepositoryPort;
import mn.demo.mini_bank.domain.model.User;

@Service
public class RegisterService {

    private static final BigDecimal INITIAL_BALANCE =
            new BigDecimal("1000000.00");

    private final UserRepositoryPort users;
    private final AccountRepositoryPort accounts;

    private final BCryptPasswordEncoder encoder =
            new BCryptPasswordEncoder();

    public RegisterService(
            UserRepositoryPort users,
            AccountRepositoryPort accounts) {

        this.users = users;
        this.accounts = accounts;
    }

    @Transactional
    public RegistrationResult register(
            String fullName,
            String username,
            String password,
            String confirmPassword) {

        String cleanName =
                fullName == null ? "" : fullName.trim();

        if (cleanName.length() < 2
                || cleanName.length() > 120) {
            throw new IllegalArgumentException(
                    "Invalid full name");
        }

        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException(
                    "Passwords do not match");
        }

        if (users.findByUsername(username).isPresent()) {
            throw new IllegalStateException(
                    "Username already exists");
        }

        User user = new User(
                UUID.randomUUID(),
                username,
                cleanName,
                encoder.encode(password),
                Instant.now()
        );

        users.save(user);

        String accountNumber = accounts.createAccount(
                user.id(),
                INITIAL_BALANCE
        ).accountNumber();

        return new RegistrationResult(
                user.id(),
                user.username(),
                accountNumber,
                INITIAL_BALANCE
        );
    }

    public record RegistrationResult(
            UUID userId,
            String username,
            String accountNumber,
            BigDecimal balance
    ) {}
}
