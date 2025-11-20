package com.sparta.payment_system.dto.order;

import com.sparta.payment_system.entity.Product;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class OrderItemCreateRequestDto {
    private Integer quantity;
}
