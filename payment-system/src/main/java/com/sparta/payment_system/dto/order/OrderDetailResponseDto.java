package com.sparta.payment_system.dto.order;

import com.sparta.payment_system.entity.Order;
import com.sparta.payment_system.entity.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class OrderDetailResponseDto {

    private final Long orderId;
    private final Long userId;
    private final BigDecimal totalAmount;
    private final Order.OrderStatus orderStatus;
    private final List<OrderItem> orderItems;

    public static OrderDetailResponseDto from(Order order) {
        return new  OrderDetailResponseDto(
                order.getOrderId(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getOrderItems()
        );
    }
}
