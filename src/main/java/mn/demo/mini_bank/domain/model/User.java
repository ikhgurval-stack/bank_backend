package mn.demo.mini_bank.domain.model;

import java.time.Instant;
import java.util.UUID;

public record User(
    UUID id,
    String username,
    String fullName,
    String passwordHash,
    Instant createdAt
) {}
