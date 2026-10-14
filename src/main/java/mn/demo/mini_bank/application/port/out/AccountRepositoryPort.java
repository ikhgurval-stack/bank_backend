package mn.demo.mini_bank.application.port.out;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import mn.demo.mini_bank.domain.model.Account;
import java.util.Set;
public interface AccountRepositoryPort {

    Account createAccount(UUID userId, BigDecimal balance);

    List<Account> findByUserId(UUID userId);

    Optional<Account> findByAccountNumber(String number);

    Optional<Account> lockById(UUID id);

    void updateBalance(UUID id, BigDecimal balance);

    
    List<Account> findByIds(Set<UUID> ids);

}
