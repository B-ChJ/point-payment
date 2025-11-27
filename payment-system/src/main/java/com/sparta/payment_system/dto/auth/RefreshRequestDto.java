package com.sparta.payment_system.dto.auth;

import lombok.Getter;

@Getter
public class RefreshRequestDto {
    private final String refreshToken;

    public RefreshRequestDto(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}