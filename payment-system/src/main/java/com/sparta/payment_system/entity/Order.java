package com.sparta.payment_system.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private OrderStatus status = OrderStatus.PENDING_PAYMENT;
    
    @CreationTimestamp
    @Column(name = "created_at",nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Payment payment;

    public enum OrderStatus {
        PENDING_PAYMENT, COMPLETED, CANCELLED
    }

    public Order(Long userId) {
        this.userId = userId;
    }

    public void setAmount(BigDecimal amount) {
        this.totalAmount = amount;
    }

    public void setOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
    }
}
