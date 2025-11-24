package com.sparta.payment_system.dto.auth;

import lombok.Getter;

@Getter
public class RefreshResponseDto {
    private final String token;

    public RefreshResponseDto(String accessToken) {
        this.token = accessToken;
    }
}