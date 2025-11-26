package com.sparta.payment_system.dto.order;

import com.sparta.payment_system.entity.OrderItem;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class OrderItemDetailResponseDto {

        private Long orderItemId;
        private String name;
        private Integer quantity;
        private BigDecimal price;
        private Long productId;

    public static OrderItemDetailResponseDto from(OrderItem orderItem) {
        return new  OrderItemDetailResponseDto(
                orderItem.getOrderItemId(),
                orderItem.getName(),
                orderItem.getQuantity(),
                orderItem.getPrice(),
                orderItem.getProduct().getProductId()
        );
    }
}
