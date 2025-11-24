package com.sparta.payment_system.service;

import com.sparta.payment_system.dto.order.OrderDetailRequestDto;
import com.sparta.payment_system.dto.order.OrderDetailResponseDto;
import com.sparta.payment_system.entity.Order;
import com.sparta.payment_system.entity.OrderItem;
import com.sparta.payment_system.entity.Product;
import com.sparta.payment_system.repository.OrderItemRepository;
import com.sparta.payment_system.repository.OrderRepository;
import com.sparta.payment_system.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    public OrderDetailResponseDto getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(
                () -> new IllegalStateException("not found Id")
        );
        return OrderDetailResponseDto.from(order);
    }

    public OrderDetailResponseDto createOrder(Long userId, OrderDetailRequestDto requestDto) {

        Order order = new Order(userId);
        orderRepository.save(order);

        // 주문 아이템 생성
        if (requestDto.getProductList().isEmpty()) {
            throw new IllegalArgumentException("상품 목록은 비어 있을 수 없습니다.");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderDetailRequestDto.ItemDto itemDto : requestDto.getProductList()) {

            Product product = productRepository.findById(itemDto.getProductId()).orElseThrow(
                    () -> new IllegalStateException("not found product Id")
            );

            BigDecimal orderItemPrice = product.getPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));

            OrderItem orderItem = new OrderItem(
                product.getName(),
                itemDto.getQuantity(),
                orderItemPrice,
                order,
                product
            );

            orderItemRepository.save(orderItem);

            order.getOrderItems().add(orderItem);

            totalAmount = totalAmount.add(orderItemPrice);
        }

        order.updateAmount(totalAmount);
        return OrderDetailResponseDto.from(order);
    }
}
