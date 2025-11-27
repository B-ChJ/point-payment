package com.sparta.payment_system.dto.refund;

import com.sparta.payment_system.entity.Refund;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class RefundResponseDto {
    private Long refundId;
    private Long paymentId;
    private BigDecimal amount;
    private String reason;
    private String status;
    private LocalDateTime refundedAt;

    public static RefundResponseDto from(Refund refund) {
        RefundResponseDto dto = new RefundResponseDto();
        dto.setRefundId(refund.getRefundId());
        // Entity 구조상 paymentId 필드를 직접 가져옵니다.
        dto.setPaymentId(refund.getPaymentId());
        dto.setAmount(refund.getAmount());
        dto.setReason(refund.getReason());
        dto.setStatus(refund.getStatus().toString());
        dto.setRefundedAt(refund.getRefundedAt());
        return dto;
    }
}