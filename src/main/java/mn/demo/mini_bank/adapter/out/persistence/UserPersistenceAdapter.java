package mn.demo.mini_bank.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import mn.demo.mini_bank.domain.model.User;
import mn.demo.mini_bank.application.port.out.UserRepositoryPort;
import mn.demo.mini_bank.adapter.out.persistence.entity.UserJpaEntity;
import mn.demo.mini_bank.adapter.out.persistence.repository.UserJpaRepository;

@Repository
public class UserPersistenceAdapter implements UserRepositoryPort {

    private final UserJpaRepository repository;

    public UserPersistenceAdapter(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return repository.findByUsername(username)
                .map(this::toDomain);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return repository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = new UserJpaEntity(
                user.id(),
                user.username(),
                user.fullName(),
                user.passwordHash(),
                user.createdAt()
        );

        return toDomain(repository.save(entity));
    }

    private User toDomain(UserJpaEntity entity) {
        return new User(
                entity.getId(),
                entity.getUsername(),
                entity.getFullName(),
                entity.getPasswordHash(),
                entity.getCreatedAt()
        );
    }
}
