package com.sparta.payment_system.dto.order;

import com.sparta.payment_system.entity.Order;
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
    private final List<OrderItemDetailResponseDto> orderItems;

    public static OrderDetailResponseDto from(Order order) {
        List<OrderItemDetailResponseDto> orderItems = order.getOrderItems().stream()
                .map(OrderItemDetailResponseDto::from)   // 여기
                .toList();

        return new  OrderDetailResponseDto(
                order.getOrderId(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getStatus(),
                orderItems
        );
    }
}
