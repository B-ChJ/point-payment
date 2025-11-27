package com.sparta.payment_system.dto.refund;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RefundRequestDto {
    private Long paymentId;
    private String reason;
}