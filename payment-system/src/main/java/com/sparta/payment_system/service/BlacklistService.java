package com.sparta.payment_system.service;

import com.sparta.payment_system.entity.BlacklistToken;
import com.sparta.payment_system.repository.BlacklistRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class BlacklistService {
    private final BlacklistRepository blacklistRepository;

    public BlacklistService(BlacklistRepository blacklistRepository) {
        this.blacklistRepository = blacklistRepository;
    }

    public void addLogoutToken(String token, LocalDateTime expiration) {
        BlacklistToken blacklistToken = new BlacklistToken(token, expiration);
        blacklistRepository.save(blacklistToken);
    }

    public boolean isBlacklisted(String refreshToken) {
        return blacklistRepository.existsByToken(refreshToken);
    }

}