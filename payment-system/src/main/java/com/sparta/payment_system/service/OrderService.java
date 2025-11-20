package com.sparta.payment_system.service;

import com.sparta.payment_system.dto.order.OrderDetailResponseDto;
import com.sparta.payment_system.entity.Order;
import com.sparta.payment_system.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderDetailResponseDto getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(
                () -> new IllegalStateException("not found Id")
        );
        return OrderDetailResponseDto.from(order);
    }
}
