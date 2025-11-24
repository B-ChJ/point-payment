package com.sparta.payment_system.repository;

import com.sparta.payment_system.entity.BlacklistToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface BlacklistRepository extends JpaRepository<BlacklistToken, Long> {
    boolean existsByToken(String token);
    Optional<BlacklistToken> findByToken(String token);
    void deleteByExpiration(LocalDateTime expiration);
}