package com.sparta.payment_system.dto.payment;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PaymentResponseDto {


    // 결제 기본 정보

    private String paymentId;        // 내부 결제 ID
    private String orderId;          // 주문 ID
    private Long userId;             // 사용자 ID
    private BigDecimal totalAmount;  // 총 결제 금액
    private String status;


    // 결제 수단 정보
    private String paymentMethod;
    private String pgProvider;
    private String pgTid;
    private String impUid;


    // 포인트 관련 정보
    private boolean usePoint;                // 포인트 사용 여부
    private BigDecimal usedPointAmount;      // 사용 포인트
    private BigDecimal earnedPointAmount;    // 적립 포인트
    private BigDecimal finalPointBalance;    // 결제 후 최종 포인트 잔액


    // 멤버십 등급 관련
    private String previousMembershipRank;   // 기존 등급
    private String updatedMembershipRank;    // 변경된 등급 (없으면 동일)


    // 주문 상품 정보
    private List<OrderItemInfo> orderItems;


    // 타임스탬프
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    // 서브 DTO (주문 아이템)
    @Getter
    @Setter
    @NoArgsConstructor
    public static class OrderItemInfo {
        private Long productId;
        private Integer quantity;
        private BigDecimal price;
        private String productName;
    }
}
