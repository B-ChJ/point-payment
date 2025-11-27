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
        log.info("Webhook Received: {}", webhookRequest);

        String paymentKey = webhookRequest.getPaymentKey();
        String status = webhookRequest.getStatus().toUpperCase();

        try {
            switch (status) {
                case "PAID" -> {
                    paymentService.completePaymentVerification(paymentKey, null);
                }
                case "FAILED" -> {
                    paymentService.failPaymentByPaymentKey(paymentKey);
                }
                case "CANCELLED", "REFUNDED" -> {
                    refundService.processWebhookRefund(paymentKey, "PG Webhook Notification");
                }
                default -> {
                    log.warn("Unknown Webhook Status: {}", status);
                    return ResponseEntity.ok("Unknown status ignored");
                }
            }
        } catch (Exception e) {
            log.error("Error processing webhook for paymentKey {}: {}", paymentKey, e.getMessage());
            return ResponseEntity.status(500).body("Error during processing");
        }

        return ResponseEntity.ok("Webhook processed successfully");
    }
}