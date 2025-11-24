package com.sparta.payment_system.service;

import com.sparta.payment_system.dto.auth.LogoutResponseDto;
import com.sparta.payment_system.entity.BlacklistToken;
import com.sparta.payment_system.repository.BlacklistRepository;
import com.sparta.payment_system.security.JwtUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;

@Service
public class BlacklistService {
    private final JwtUtil jwtUtil;
    private final BlacklistRepository blacklistRepository;

    public BlacklistService(JwtUtil jwtUtil, BlacklistRepository blacklistRepository) {
        this.jwtUtil = jwtUtil;
        this.blacklistRepository = blacklistRepository;
    }

    public LogoutResponseDto addLogoutToken(String token) {
        Date expirationDate = jwtUtil.getExpiration(token);
        LocalDateTime expiration = new java.sql.Timestamp(expirationDate.getTime()).toLocalDateTime();

        BlacklistToken blacklistToken = new BlacklistToken(token, expiration);
        blacklistRepository.save(blacklistToken);

        return LogoutResponseDto.save(true);
    }

    public boolean isBlacklisted(String refreshToken) {
        return blacklistRepository.existsByToken(refreshToken);
    }

}
