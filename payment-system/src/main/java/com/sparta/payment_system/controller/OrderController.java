package com.sparta.payment_system.controller;

import com.sparta.payment_system.dto.order.OrderDetailRequestDto;
import com.sparta.payment_system.dto.order.OrderDetailResponseDto;
import com.sparta.payment_system.entity.Order;
import com.sparta.payment_system.repository.OrderRepository;
import com.sparta.payment_system.security.CustomUserDetails;
import com.sparta.payment_system.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    
    @PostMapping
    public ResponseEntity<OrderDetailResponseDto> createOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody OrderDetailRequestDto requestDto ) {
        OrderDetailResponseDto responseDto = orderService.createOrder(userDetails.getId(), requestDto);
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        try {
            List<Order> orders = orderRepository.findAll();
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable Long orderId) {
        try {
            Optional<Order> order = orderRepository.findByOrderId(orderId);
            return order.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PutMapping("/{orderId}")
    public ResponseEntity<Order> updateOrder(@PathVariable Long orderId, @RequestBody Order orderDetails) {
        try {
            Optional<Order> orderOptional = orderRepository.findByOrderId(orderId);
            if (orderOptional.isPresent()) {
                Order order = orderOptional.get();
                order.setUserId(orderDetails.getUserId());
                order.setTotalAmount(orderDetails.getTotalAmount());
                order.setStatus(orderDetails.getStatus());
                
                Order updatedOrder = orderRepository.save(order);
                return ResponseEntity.ok(updatedOrder);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long orderId) {
        try {
            Optional<Order> order = orderRepository.findByOrderId(orderId);
            if (order.isPresent()) {
                orderRepository.delete(order.get());
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getOrdersByUser(@PathVariable Long userId) {
        try {
            List<Order> orders = orderRepository.findByUserId(userId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Order>> getOrdersByStatus(@PathVariable Order.OrderStatus status) {
        try {
            List<Order> orders = orderRepository.findByStatus(status);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/user/{userId}/status/{status}")
    public ResponseEntity<List<Order>> getOrdersByUserAndStatus(
            @PathVariable Long userId, 
            @PathVariable Order.OrderStatus status) {
        try {
            List<Order> orders = orderRepository.findByUserIdAndStatus(userId, status);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }


}
