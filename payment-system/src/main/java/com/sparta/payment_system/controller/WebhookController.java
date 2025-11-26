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
                    // 결제 완료는 PaymentService로 위임
                    paymentService.completePaymentByPaymentKey(paymentKey,null);
                }
                case "FAILED" -> {
                    // 결제 실패는 PaymentService로 위임
                    paymentService.failPaymentByPaymentKey(paymentKey);
                }
                case "CANCELLED", "REFUNDED" -> {
                    // 환불/취소는 RefundService로 위임 (SRP 준수)
                    refundService.processWebhookRefund(paymentKey, "PG Webhook Notification");
                }
                default -> {
                    log.warn("Unknown Webhook Status: {}", status);
                    return ResponseEntity.ok("Unknown status ignored");
                }
            }
        } catch (Exception e) {
            log.error("Error processing webhook for paymentKey {}: {}", paymentKey, e.getMessage());
            // PG사 재시도를 막기 위해 성공 응답을 보낼 수도 있으나, 여기서는 오류를 명확히 알림
            return ResponseEntity.status(500).body("Error during processing");
        }

        return ResponseEntity.ok("Webhook processed");
    }
}

