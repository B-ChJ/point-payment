package com.sparta.payment_system.controller;

import com.sparta.payment_system.dto.payment.PaymentRequestDto;
import com.sparta.payment_system.dto.payment.PaymentResponseDto;
import com.sparta.payment_system.service.PaymentService;
import com.sparta.payment_system.dto.payment.PortOnePaymentReadyResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // Security import
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // 1. 주문에 대해 결제 생성
    @PostMapping("/orders/{orderId}/payments")
    public ResponseEntity<PortOnePaymentReadyResponseDto> createPayment(
            @PathVariable Long orderId,
            @RequestBody PaymentRequestDto requestDto,
            Authentication authentication
    ) {

        // 현재 로그인된 사용자 ID를 추출하여 Service로 전달 (권한 검증용)
        String currentUserIdentifier = authentication.getName();

        PortOnePaymentReadyResponseDto dto = paymentService.createPayment(
                orderId,
                requestDto,
                currentUserIdentifier
        );

        return ResponseEntity.ok(dto);
    }

    // 2. 결제 성공 처리 (주로 클라이언트 측에서 요청)
    @PostMapping("/payments/complete")
    public ResponseEntity<PaymentResponseDto> completePayment(
            @RequestParam String paymentKey,Authentication authentication) {
        Long currentUserId = Long.parseLong(authentication.getName());

        PaymentResponseDto dto = paymentService.completePaymentByPaymentKey(paymentKey,currentUserId);
        return ResponseEntity.ok(dto);
    }

    // 3. 결제 내역 조회
    @GetMapping("/payments/{paymentId}")
    public ResponseEntity<PaymentResponseDto> getPayment(@PathVariable Long paymentId) {

        PaymentResponseDto dto = paymentService.getPayment(paymentId);
        return ResponseEntity.ok(dto);
    }
}