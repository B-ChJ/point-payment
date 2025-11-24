package com.sparta.payment_system.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "blacklisted_token")
public class BlacklistToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String token;

    @Column(nullable = false)
    private LocalDateTime expiration;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public BlacklistToken(String token, LocalDateTime expiration) {
        this.token = token;
        this.expiration = expiration;
    }

    public BlacklistToken() {}
}