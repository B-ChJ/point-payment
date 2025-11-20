package com.sparta.payment_system.service;

import com.sparta.payment_system.dto.order.OrderItemCreateRequestDto;
import com.sparta.payment_system.dto.order.OrderItemCreateResponseDto;
import com.sparta.payment_system.entity.Order;
import com.sparta.payment_system.entity.OrderItem;
import com.sparta.payment_system.entity.Product;
import com.sparta.payment_system.repository.OrderItemRepository;
import com.sparta.payment_system.repository.OrderRepository;
import com.sparta.payment_system.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class OrderItemService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderItemCreateResponseDto createOrderItem(long orderId, Long productId, OrderItemCreateRequestDto requestDto) {

        Product product = productRepository.findById(productId).orElseThrow(
                () -> new IllegalArgumentException("not found product")
        );

        //테스트용 임시 주문 생성
        Long userId = 1L;
        Order order = new Order(
                userId,
                product.getPrice().multiply(BigDecimal.valueOf(requestDto.getQuantity())));
        orderRepository.save(order);

        OrderItem orderItem = new OrderItem(
                product.getName(),
                requestDto.getQuantity(),
                product.getPrice().multiply(BigDecimal.valueOf(requestDto.getQuantity())),
                order,
                product
                );

        orderItemRepository.save(orderItem);

        return OrderItemCreateResponseDto.from(orderItem);
    }
}
