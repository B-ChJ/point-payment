package com.sparta.payment_system.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentVerificationDto {
    // PortOne의 imp_uid
    private String paymentKey;

    // 주문 번호
    private String orderId;

    // 결제된 금액
    private BigDecimal amount;

    //결제 상태
    private String status;
}