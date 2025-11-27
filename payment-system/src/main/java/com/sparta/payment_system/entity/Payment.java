package com.sparta.payment_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 결제 정보를 나타내는 엔티티입니다.
 * 주문(Order)과 1:1 관계를 가지며, 결제 상태 및 금액 정보를 포함합니다.
 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
public class Payment {

    /**
     * 결제 식별자 (Primary Key)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    /**
     * 연결된 주문 엔티티 (1:1 관계)
     */
    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;


    /**
     * PG사에서 발급한 결제 고유 키 (PortOne Payment Key 등)
     */
    @Column(unique = true)
    private String paymentKey;

    /**
     * 결제 수단 식별자 (PaymentMethod Enum의 ID)
     */
    @Column
    private Long methodId;

    /**
     * 사용된 포인트 금액
     */
    @Column(precision = 19, scale = 2)
    private BigDecimal pointsUsed = BigDecimal.ZERO;

    /**
     * 기타 할인 금액
     */
    @Column(precision = 19, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /**
     * 최종 실 결제 금액 (총 금액 - 포인트 사용 - 할인 금액)
     */
    @Column(precision = 19, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    /**
     * 결제 상태 (READY, PAID, FAILED, REFUNDED)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    /**
     * 레코드 생성 일시
     */
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /**
     * 레코드 마지막 수정 일시
     */
    @LastModifiedDate
    private LocalDateTime updatedAt;

    /**
     * 실제 결제 완료 일시
     */
    private LocalDateTime paidAt;

    /**
     * 이 결제에 연결된 환불 내역 리스트 (1:N 관계)
     */
    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Refund> refunds;

    /**
     * 결제 상태를 정의하는 Enum.
     */
    public enum PaymentStatus {
        READY, PAID, FAILED, REFUNDED
    }

    /**
     * 결제 수단을 정의하는 Enum.
     */
    public enum PaymentMethod {
        CARD(1L), BANK_TRANSFER(2L), POINT(3L);

        private final Long id;
        PaymentMethod(Long id) { this.id = id; }
        public Long getId() { return id; }
        public static PaymentMethod fromId(Long id) {
            for (PaymentMethod method : values()) {
                if (method.getId().equals(id)) return method;
            }
            throw new IllegalArgumentException("Invalid PaymentMethod ID:" + id);
        }
    }
}