package com.sparta.payment_system.dto.payment;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PaymentRequestDto {
    
    // 주문 정보
    private String orderId;
    private Long userId;
    private BigDecimal totalAmount;
    private String orderName;
    
    // 주문 아이템들
    private List<OrderItemDto> orderItems;
    
    // 결제 정보
    private String paymentMethod;
    private String impUid;

    // 포인트 관련
    private boolean usePoint;
    private BigDecimal usePointAmount;
    
    // 사용자 정보 (선택적)
    private String customerName;
    private String customerEmail;
    private String customerPhone;

    //맴버심 등급 정보
    private String currentMembershipRank;
    
    @Getter
    @Setter
    @NoArgsConstructor
    public static class OrderItemDto {
        private Long productId;
        private Integer quantity;
        private BigDecimal price;
        private String productName;
    }
}
