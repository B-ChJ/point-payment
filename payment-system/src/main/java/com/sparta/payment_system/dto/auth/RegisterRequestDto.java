package com.sparta.payment_system.dto.auth;

import lombok.Getter;

@Getter
public class RegisterRequestDto {
    private String email;
    private String passwordHash;
    private String name;
}
