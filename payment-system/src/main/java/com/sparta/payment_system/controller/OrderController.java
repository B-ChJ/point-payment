package com.sparta.payment_system.controller;

import com.sparta.payment_system.dto.order.OrderDetailRequestDto;
import com.sparta.payment_system.dto.order.OrderDetailResponseDto;
import com.sparta.payment_system.security.CustomUserDetails;
import com.sparta.payment_system.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    
    @PostMapping
    public ResponseEntity<OrderDetailResponseDto> createOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody OrderDetailRequestDto requestDto ) {
        OrderDetailResponseDto responseDto = orderService.createOrder(userDetails.getId(), requestDto);
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponseDto> getOrder(@PathVariable Long orderId) {
        OrderDetailResponseDto responseDto = orderService.getOrder(orderId);
        return ResponseEntity.ok(responseDto);
    }

}
