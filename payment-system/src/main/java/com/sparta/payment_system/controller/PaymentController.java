package com.sparta.payment_system.controller;

import com.sparta.payment_system.entity.Payment;
import com.sparta.payment_system.entity.Refund;
import com.sparta.payment_system.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 1️⃣ 통합 결제
     * 주문에 대해 결제 생성
     */
    @PostMapping("/orders/{orderId}/payments")
    public ResponseEntity<Payment> createPayment(
            @PathVariable Long orderId,
            @RequestParam(required = false) boolean usePoints) {

        Payment payment = paymentService.createPayment(orderId, usePoints);
        return ResponseEntity.ok(payment);
    }

    /**
     * 2️⃣ 결제 성공 처리
     * 외부 결제 승인 완료 시 호출
     */
    @PostMapping("/payments/{paymentId}/complete")
    public ResponseEntity<Payment> completePayment(@PathVariable Long paymentId) {
        Payment payment = paymentService.completePayment(paymentId);
        return ResponseEntity.ok(payment);
    }

    /**
     * 3️⃣ 결제 내역 조회
     */
    @GetMapping("/payments/{paymentId}")
    public ResponseEntity<Payment> getPayment(@PathVariable Long paymentId) {
        Payment payment = paymentService.getPayment(paymentId);
        return ResponseEntity.ok(payment);
    }

    /**
     * 4️⃣ 결제 환불 요청
     */
    @PostMapping("/payments/{paymentId}/refund")
    public ResponseEntity<Refund> refundPayment(
            @PathVariable Long paymentId,
            @RequestParam(required = false) String reason) {

        Refund refund = paymentService.refundPayment(paymentId, reason);
        return ResponseEntity.ok(refund);
    }
}


