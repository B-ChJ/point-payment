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

    @Column(nullable = false)
    private String orderId;

    @Column(unique = true)
    private String paymentKey;

    @Column
    private Long methodId;

    @Column(precision = 19, scale = 2)
    private BigDecimal pointsUsed = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    private LocalDateTime paidAt;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Refund> refunds;

    public enum PaymentStatus {
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
