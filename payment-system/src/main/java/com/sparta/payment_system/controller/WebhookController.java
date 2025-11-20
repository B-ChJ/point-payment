package com.sparta.payment_system.controller;

import com.sparta.payment_system.dto.webhook.WebhookRequestDto;
import com.sparta.payment_system.entity.Payment;
import com.sparta.payment_system.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final PaymentService paymentService;

    @PostMapping("/payment")
    public ResponseEntity<String> handleWebhook(@RequestBody WebhookRequestDto webhookRequest) {
        String paymentKey = webhookRequest.getPaymentKey();
        String status = webhookRequest.getStatus();

        Payment payment = paymentService.getPaymentByPaymentKey(paymentKey);

        switch (status) {
            case "PAID" -> paymentService.completePayment(payment.getPaymentId());
            case "FAILED" -> paymentService.failPayment(payment.getPaymentId());
            case "REFUNDED" -> paymentService.refundPayment(payment.getPaymentId(), "Webhook Refund");
            default -> {
                return ResponseEntity.badRequest().body("Unknown status");
            }
        }

        return ResponseEntity.ok("Webhook processed");
    }
}




