package mn.demo.mini_bank.adapter.out.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import mn.demo.mini_bank.adapter.out.persistence.entity.UserJpaEntity;

public interface UserJpaRepository
        extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByUsername(String username);
}
