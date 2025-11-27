package com.sparta.payment_system.repository;

import com.sparta.payment_system.entity.Payment;
import com.sparta.payment_system.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    /**
     * 특정 결제 ID(PK)에 대한 모든 환불 기록을 조회합니다.
     */
    List<Refund> findByPaymentId(Long paymentId);

    /**
     * 특정 상태의 모든 환불 기록을 조회합니다.
     */
    List<Refund> findByStatus(Refund.RefundStatus status);

    /**
     * 💡 특정 Payment 엔티티와 연결된 환불 기록을 조회합니다.
     * (RefundService에서 중복 처리 방지 및 기존 기록 반환을 위해 사용됩니다.)
     */
    Optional<Refund> findByPayment(Payment payment);
}