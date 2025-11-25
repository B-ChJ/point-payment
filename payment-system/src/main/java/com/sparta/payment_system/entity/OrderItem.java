package com.sparta.payment_system.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long orderItemId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    public OrderItem(String name, Integer quantity, BigDecimal price, Product product) {
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.product = product;
    }

}
