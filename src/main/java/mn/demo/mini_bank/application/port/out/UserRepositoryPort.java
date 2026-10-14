package mn.demo.mini_bank.application.port.out;

import java.util.Optional;
import java.util.UUID;
import mn.demo.mini_bank.domain.model.User;

public interface UserRepositoryPort {

    Optional<User> findByUsername(String username);

    Optional<User> findById(UUID id);

    User save(User user);
}
