package com.sparta.payment_system.repository;

import com.sparta.payment_system.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    Optional<Payment> findByOrder_OrderId(Long orderId);


    Optional<Payment> findByPaymentKey(String paymentKey);
    
    List<Payment> findByStatus(Payment.PaymentStatus status);
    
    List<Payment> findByMethodId(Long methodId);

    List<Payment> findAllByOrderUserId(Long userId);
}
