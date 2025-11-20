package com.sparta.payment_system.entity;

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
    private Long paymentId;

    @Column(name = "order_id", nullable = false, length = 255)

    @Column(nullable = false)
    private String orderId;

    @Column(name = "method_id")

    @Column(unique = true)
    private String paymentKey;

    @Column
    private Long methodId;

    @Column(name = "imp_uid", unique = true, length = 255)
    private String impUid;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;


    @Column(precision = 19, scale = 2)
    private BigDecimal pointsUsed = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "payment_method", length = 100)
    private String paymentMethod;

    @Column(name = "paid_at")

    private LocalDateTime paidAt;


    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Refund> refunds;


    public enum PaymentStatus {
        PAID, FAILED, REFUNDED, PARTIALLY_REFUNDED
    }
        PAID, FAILED, REFUNDED
    }

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
