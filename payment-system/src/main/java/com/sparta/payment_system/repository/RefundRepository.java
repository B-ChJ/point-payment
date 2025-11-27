package com.sparta.payment_system.repository;

import com.sparta.payment_system.entity.Payment;
import com.sparta.payment_system.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Refund 엔티티에 대한 데이터 접근을 처리하는 JPA 리포지토리 인터페이스입니다.
 */
@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    /**
     * 특정 결제 ID에 연결된 모든 환불 기록을 조회합니다.
     */
    List<Refund> findByPaymentId(Long paymentId);

    /**
     * 특정 상태를 가진 모든 환불 기록을 조회합니다.
     */
    List<Refund> findByStatus(Refund.RefundStatus status);

    /**
     * 특정 Payment 엔티티와 연결된 환불 기록을 조회합니다.
     */
    Optional<Refund> findByPayment(Payment payment);
}