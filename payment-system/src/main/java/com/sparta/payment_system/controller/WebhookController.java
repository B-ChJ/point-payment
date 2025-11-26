package com.sparta.payment_system.controller;

import com.sparta.payment_system.dto.webhook.WebhookRequestDto;
import com.sparta.payment_system.service.PaymentService;
import com.sparta.payment_system.service.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final PaymentService paymentService;
    private final RefundService refundService;

    @PostMapping("/payment")
    public ResponseEntity<String> handleWebhook(@RequestBody WebhookRequestDto webhookRequest) {
        log.info("Webhook Received: {}", webhookRequest); // 들어온 데이터 확인용 로그

        String paymentKey = webhookRequest.getPaymentKey();

        String status = webhookRequest.getStatus().toUpperCase();

        Payment payment = paymentService.getPaymentByPaymentKey(paymentKey);

        switch (status) {
            case "PAID" -> paymentService.completePayment(payment.getPaymentId());
            case "FAILED" -> paymentService.failPayment(payment.getPaymentId());
            case "CANCELLED", "REFUNDED" -> paymentService.refundPayment(payment.getPaymentId(), "Webhook Refund");
            default -> {
                log.warn("Unknown Webhook Status: {}", status);
                return ResponseEntity.ok("Unknown status ignored");
            }
        }

        return ResponseEntity.ok("Webhook processed");
    }
}



