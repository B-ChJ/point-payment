package com.sparta.payment_system.dto.auth;

import com.sparta.payment_system.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenResponseDto {
    private final String AccessToken;
    private final String RefreshToken;
    private final User user;
}
