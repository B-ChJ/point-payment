package com.sparta.payment_system.dto.payment;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

/**
 * PortOne SDK 호출을 위해 PaymentService.createPayment가 프론트엔드에 반환하는 DTO.
 */
@Getter
@Setter
public class PortOnePaymentReadyResponseDto {

    private Long paymentId;
    private String paymentKey;
    private BigDecimal amount;
    private String orderName;
}
