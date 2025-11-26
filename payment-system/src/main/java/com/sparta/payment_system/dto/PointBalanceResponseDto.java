package com.sparta.payment_system.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class PointBalanceResponseDto {
    private Long userId;
    private String email;
    private BigDecimal totalPoints;
}

