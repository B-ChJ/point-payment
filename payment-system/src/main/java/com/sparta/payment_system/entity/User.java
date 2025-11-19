package com.sparta.payment_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "name")
    private String name;

    @Column(name = "total_payment_amount")
    private BigDecimal totalPaymentAmount = BigDecimal.ZERO;

    @Column(name = "total_points", nullable = false)
    private BigDecimal totalPoints = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership",nullable = false)
    private MembershipRank membershipRank;

    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.USER;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<PointTransaction> pointTransactions = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public User(String email, String passwordHash, String name) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
    }

    public enum UserRole {
        ADMIN, USER
    }
}
