package com.sparta.payment_system.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MyInfoResponseDto {
    private Long userId;
    private String email;
    private String name;
    private String membershipLevel;
}
