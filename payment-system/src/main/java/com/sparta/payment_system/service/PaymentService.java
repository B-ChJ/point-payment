package com.sparta.payment_system.service;

import com.sparta.payment_system.entity.*;
import com.sparta.payment_system.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final RefundRepository refundRepository;
    private final PointTransactionRepository pointTransactionRepository;

    @Transactional
    public Payment createPayment(Long orderId, boolean usePoints) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid orderId"));
        User user = userRepository.findById(order.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid userId"));

        BigDecimal totalAmount = order.getTotalAmount();
        BigDecimal pointsUsed = BigDecimal.ZERO;

        if (usePoints) {
            pointsUsed = user.getTotalPoints().min(totalAmount);
            totalAmount = totalAmount.subtract(pointsUsed);
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(totalAmount);
        payment.setPointsUsed(pointsUsed);
        payment.setDiscountAmount(BigDecimal.ZERO);
        payment.setStatus(Payment.PaymentStatus.FAILED);
        payment.setMethodId(Payment.PaymentMethod.CARD.getId());

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment completePayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid paymentId"));
        if (payment.getStatus() == Payment.PaymentStatus.PAID) return payment;

        payment.setStatus(Payment.PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());

        User user = getUserByPayment(payment);

        // 포인트 차감
        if (payment.getPointsUsed().compareTo(BigDecimal.ZERO) > 0) {
            user.setTotalPoints(user.getTotalPoints().subtract(payment.getPointsUsed()));
            userRepository.save(user);

            PointTransaction transaction = new PointTransaction();
            transaction.setUser(user);
            transaction.setPointsChanged(payment.getPointsUsed());
            transaction.setType(PointTransaction.PointType.USED);
            transaction.setCreatedAt(LocalDateTime.now());
            pointTransactionRepository.save(transaction);
        }

        // 포인트 적립 1%
        BigDecimal earnedPoints = payment.getAmount().multiply(new BigDecimal("0.01"));
        if (earnedPoints.compareTo(BigDecimal.ZERO) > 0) {
            user.setTotalPoints(user.getTotalPoints().add(earnedPoints));
            userRepository.save(user);

            PointTransaction earned = new PointTransaction();
            earned.setUser(user);
            earned.setPointsChanged(earnedPoints);
            earned.setType(PointTransaction.PointType.EARNED);
            earned.setCreatedAt(LocalDateTime.now());
            pointTransactionRepository.save(earned);
        }

        updateMembershipLevel(user);

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment failPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid paymentId"));
        payment.setStatus(Payment.PaymentStatus.FAILED);
        return paymentRepository.save(payment);
    }

    @Transactional
    public Refund refundPayment(Long paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid paymentId"));

        if (payment.getStatus() != Payment.PaymentStatus.PAID)
            throw new IllegalStateException("Payment is not completed, cannot refund");

        Refund refund = new Refund();
        refund.setPayment(payment);
        refund.setAmount(payment.getAmount());
        refund.setReason(reason);
        refund.setStatus(Refund.RefundStatus.REQUESTED);
        refundRepository.save(refund);

        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        User user = getUserByPayment(payment);

        // 포인트 복구
        if (payment.getPointsUsed().compareTo(BigDecimal.ZERO) > 0) {
            user.setTotalPoints(user.getTotalPoints().add(payment.getPointsUsed()));
            userRepository.save(user);

            PointTransaction transaction = new PointTransaction();
            transaction.setUser(user);
            transaction.setPointsChanged(payment.getPointsUsed());
            transaction.setType(PointTransaction.PointType.EARNED);
            transaction.setCreatedAt(LocalDateTime.now());
            pointTransactionRepository.save(transaction);
        }

        return refund;
    }

    /**
     * PortOne 환불용: paymentKey 기준, Mono<Boolean> 반환
     */
    @Transactional
    public Mono<Boolean> cancelPayment(String paymentKey, String reason) {
        try {
            Payment payment = paymentRepository.findByPaymentKey(paymentKey)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid paymentKey"));

            if (payment.getStatus() != Payment.PaymentStatus.PAID) {
                return Mono.just(false);
            }

            // 환불 처리
            Refund refund = new Refund();
            refund.setPayment(payment);
            refund.setAmount(payment.getAmount());
            refund.setReason(reason);
            refund.setStatus(Refund.RefundStatus.COMPLETED);
            refundRepository.save(refund);

            // 결제 상태 변경
            payment.setStatus(Payment.PaymentStatus.REFUNDED);
            paymentRepository.save(payment);

            // 포인트 복구
            User user = getUserByPayment(payment);
            if (payment.getPointsUsed().compareTo(BigDecimal.ZERO) > 0) {
                user.setTotalPoints(user.getTotalPoints().add(payment.getPointsUsed()));
                userRepository.save(user);

                PointTransaction transaction = new PointTransaction();
                transaction.setUser(user);
                transaction.setPointsChanged(payment.getPointsUsed());
                transaction.setType(PointTransaction.PointType.EARNED);
                transaction.setCreatedAt(LocalDateTime.now());
                pointTransactionRepository.save(transaction);
            }

            return Mono.just(true);

        } catch (Exception e) {
            e.printStackTrace();
            return Mono.just(false);
        }
    }

    @Transactional(readOnly = true)
    public Payment getPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid paymentId"));
    }

    @Transactional(readOnly = true)
    public Payment getPaymentByPaymentKey(String paymentKey) {
        return paymentRepository.findByPaymentKey(paymentKey)
                .orElseThrow(() -> new IllegalArgumentException("Invalid paymentKey"));
    }

    private User getUserByPayment(Payment payment) {
        Order order = payment.getOrder();
        if (order == null) throw new IllegalArgumentException("Payment has no associated order");
        return userRepository.findById(order.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid userId"));
    }

    private void updateMembershipLevel(User user) {
        BigDecimal totalSpent = paymentRepository.findAllByOrderUserId(user.getUserId())
                .stream()
                .filter(p -> p.getStatus() == Payment.PaymentStatus.PAID)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalSpent.compareTo(new BigDecimal("100000")) >= 0) {
            user.setMembershipRank(MembershipRank.VVIP);
        } else if (totalSpent.compareTo(new BigDecimal("50000")) >= 0) {
            user.setMembershipRank(MembershipRank.VIP);
        } else {
            user.setMembershipRank(MembershipRank.NORMAL);
        }

        userRepository.save(user);
    }
}







