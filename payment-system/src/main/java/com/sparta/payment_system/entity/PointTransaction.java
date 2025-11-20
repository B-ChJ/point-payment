package com.sparta.payment_system.entity;

import com.sparta.payment_system.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class PointTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 포인트 거래와 관련된 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // 포인트 사용 또는 적립 금액
    private int amount;

    // 거래 타입: "EARN" 또는 "USE"
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PointType type;

    @Column(nullable = false)
    private BigDecimal pointsChanged;
    // 거래 생성 시간
    private LocalDateTime createdAt = LocalDateTime.now();

    // 생성자 (필요 시 추가)
    public PointTransaction(User user, int amount, PointType type) {
        this.user = user;
        this.amount = amount;
        this.type = type;
        this.createdAt = LocalDateTime.now();
    }
    public enum PointType {
        USED, // 사용
        EARNED // 적립
    }
}
