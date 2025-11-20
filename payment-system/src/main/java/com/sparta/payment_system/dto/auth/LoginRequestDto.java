package com.sparta.payment_system.dto.auth;

import lombok.Getter;

@Getter
public class LoginRequestDto {
    private String email;
    private String password;
}
