package com.sparta.payment_system.controller;

import com.sparta.payment_system.entity.Payment;
import com.sparta.payment_system.entity.Refund;
import com.sparta.payment_system.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // 주문에 대해 결제 생성
    @PostMapping("/orders/{orderId}/payments")
    public ResponseEntity<Payment> createPayment(
            @PathVariable Long orderId,
            @RequestParam(required = false) boolean usePoints) {

        // Payment 엔티티를 생성하고 PG사에 넘겨줄 금액 정보를 포함한 응답을 반환
        Payment payment = paymentService.createPayment(orderId, usePoints);
        return ResponseEntity.ok(payment);
    }

    // 결제 성공 처리
    @PostMapping("/payments/complete")
    public ResponseEntity<Payment> completePayment(@RequestParam String paymentKey) {

        // Service에서 paymentKey로 Payment를 찾고 검증 로직 수행
        Payment payment = paymentService.completePaymentByPaymentKey(paymentKey);
        return ResponseEntity.ok(payment);
    }

    // 결제 내역 조회
    @GetMapping("/payments/{paymentId}")
    public ResponseEntity<Payment> getPayment(@PathVariable Long paymentId) {
        Payment payment = paymentService.getPayment(paymentId);
        return ResponseEntity.ok(payment);
    }

    // 결제 환불 요청
    @PostMapping("/payments/{paymentId}/refund")
    public ResponseEntity<Refund> refundPayment(
            @PathVariable Long paymentId,
            @RequestParam(required = false) String reason) {

        Refund refund = paymentService.refundPayment(paymentId, reason);
        return ResponseEntity.ok(refund);
    }
}

