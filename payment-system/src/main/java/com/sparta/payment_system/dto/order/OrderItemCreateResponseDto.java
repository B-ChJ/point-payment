package com.sparta.payment_system.dto.order;

import com.sparta.payment_system.entity.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class OrderItemCreateResponseDto {
    private Long orderItemId;
    private String name;
    private BigDecimal price;
    private Integer quantity;
    private Long orderId;
    private Long productId;

    public static OrderItemCreateResponseDto from(OrderItem orderItem) {
        return  new OrderItemCreateResponseDto(
                orderItem.getOrderItemId(),
                orderItem.getName(),
                orderItem.getPrice(),
                orderItem.getQuantity(),
                orderItem.getOrder().getOrderId(),
                orderItem.getProduct().getProductId()
        );
    }
}
