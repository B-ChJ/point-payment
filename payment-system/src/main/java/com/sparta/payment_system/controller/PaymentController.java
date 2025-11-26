package com.sparta.payment_system.controller;

import com.sparta.payment_system.dto.payment.PaymentRequestDto;
import com.sparta.payment_system.dto.payment.PaymentResponseDto;
import com.sparta.payment_system.service.PaymentService;
import com.sparta.payment_system.dto.payment.PortOnePaymentReadyResponseDto;
import com.sparta.payment_system.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // 1. 주문에 대해 결제 생성
    @PostMapping("/{orderId}/payments")
    public ResponseEntity<PortOnePaymentReadyResponseDto> createPayment(
            @PathVariable Long orderId,
            @RequestBody PaymentRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        Long currentUserId = userDetails.getId();

        PortOnePaymentReadyResponseDto dto = paymentService.createPaymentReady(
                currentUserId,
                orderId,
                requestDto
        );

        return ResponseEntity.ok(dto);
    }

    // 2. 결제 성공 처리
    @PostMapping("/complete")
    public ResponseEntity<PaymentResponseDto> completePayment(
            @RequestParam String paymentKey,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {


        Long currentUserId = userDetails.getId();

        PaymentResponseDto dto = paymentService.completePaymentVerification(
                paymentKey,
                currentUserId
        );
        return ResponseEntity.ok(dto);
    }

    // 3. 결제 내역 조회
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponseDto> getPayment(
            @PathVariable Long paymentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long currentUserId = userDetails.getId();


        PaymentResponseDto dto = paymentService.getPaymentDetails(paymentId, currentUserId);
        return ResponseEntity.ok(dto);
    }
}