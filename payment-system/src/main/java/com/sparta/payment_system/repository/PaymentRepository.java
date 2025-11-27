package com.sparta.payment_system.repository;

import com.sparta.payment_system.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Payment 엔티티에 대한 데이터 접근을 처리하는 JPA 리포지토리 인터페이스입니다.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * 주문 ID를 사용하여 결제 엔티티를 조회합니다.
     */
    Optional<Payment> findByOrder_OrderId(Long orderId);

    /**
     * PG사 결제 고유 키(PaymentKey)를 사용하여 결제 엔티티를 조회합니다.
     */
    Optional<Payment> findByPaymentKey(String paymentKey);

    /**
     * 특정 결제 상태를 가진 모든 결제 엔티티를 조회합니다.
     */
    List<Payment> findByStatus(Payment.PaymentStatus status);

    /**
     * 특정 결제 수단 ID를 가진 모든 결제 엔티티를 조회합니다.
     */
    List<Payment> findByMethodId(Long methodId);

    /**
     * 특정 사용자 ID의 주문에 연결된 모든 결제 엔티티를 조회합니다.
     */
    List<Payment> findAllByOrderUserId(Long userId);
}