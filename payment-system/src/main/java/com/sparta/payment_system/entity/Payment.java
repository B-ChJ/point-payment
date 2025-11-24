package com.sparta.payment_system.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    //주문정보
    @Column(name = "order_id", nullable = false, length = 255)
    private String orderId;


    @Column(name = "method_id")

    @Column(unique = true)
    private String paymentKey;

    @Column
    private Long methodId;

    //사용한 포인트
    @Column(name = "points_used", precision = 10, scale = 2)
    private BigDecimal pointsUsed;

    @Column(name = "discount_amount", nullable = false)
    private BigDecimal discountAmount;

    //금액정보
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal Amount;

    //결제 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private PaymentStatus status;

    //결제 완료 시각
    @Column(name = "paid_at")
    private LocalDateTime paidAt;
    
    // 외래키 제약조건 문제를 방지하기 위해 일시적으로 주석 처리
    // @OneToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "order_id", referencedColumnName = "order_id", insertable = false, updatable = false)
    // @JsonBackReference
    // private Order order;

    //환불 내역
    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Refund> refunds;

    //결제 상태
    public enum PaymentStatus {
        PAID, //결제 완료
        FAILED, // 결제 실패
        REFUNDED, //환불
    }

    //결제 수단
    public enum PaymentMethod {
        CARD(1L),
        BANK_TRANSFER(2L),
        POINT(3L);

        private final Long id;

        PaymentMethod(Long id) {
            this.id = id;
        }

        public Long getId() {
            return id;
        }

        //DB의 methodId를 Enum으로 반환
        public static PaymentMethod fromId(Long id) {
            for (PaymentMethod method : values()) {
                if (method.getId().equals(id)) return method;
            }
            throw new IllegalArgumentException("Invalid PaymentMethod ID:" + id);
        }
    }

    //DB의 methoID 기반으로 Enum 세팅
    @Transient
    private PaymentMethod paymentMethod;

    @PostLoad
    public void setPaymentMethodEnum() {
        if (methodId != null) this.paymentMethod = PaymentMethod.fromId(methodId);
    }
}
