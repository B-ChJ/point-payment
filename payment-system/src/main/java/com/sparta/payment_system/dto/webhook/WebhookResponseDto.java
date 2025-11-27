package com.sparta.payment_system.dto.webhook;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WebhookResponseDto {

    private boolean success;   // 처리 성공 여부
    private String message;    // 처리 결과 메시지
    private String status;     // DB에 기록된 결제 상태(paid/failed/cancelled)
    private String orderId;    // merchantUid
    private String paymentId;  // impUid
}

