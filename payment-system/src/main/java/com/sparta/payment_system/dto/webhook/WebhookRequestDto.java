package com.sparta.payment_system.dto.webhook;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WebhookRequestDto {
    private String paymentKey;
    private String status;  // PAID / FAILED / REFUNDED
    private String orderId; // 필요 시
}


