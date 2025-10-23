package com.ada.proj.repository;

import com.ada.proj.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    Optional<RefreshToken> findByUuid(String uuid);
    void deleteByUuid(String uuid);
    void deleteByExpiresAtBefore(Instant instant);
}
